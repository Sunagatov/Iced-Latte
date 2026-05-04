# Stripe Integration Plan — Iced Latte

**Date:** 2026-05-04
**Scope:** Backend (Java 25 / Spring Boot 4) + Frontend (Next.js 16)
**Stripe Mode:** Hosted Checkout Sessions (redirect-based)
**Architecture:** Backend-owned order-first — Iced Latte owns the order lifecycle; Stripe only handles payment collection.

---

## Executive Summary

Iced Latte will integrate Stripe using a **backend-owned order-first** architecture. The backend creates a local Order with status `PENDING_PAYMENT` *before* creating the Stripe Checkout Session. Stripe collects the payment; the webhook transitions the order to `PAID`. The order, cart snapshot, pricing, delivery details, and payment state machine all live in Iced Latte's database — Stripe is an external payment rail, not the order system.

This design gives the project real senior-level backend engineering: domain modelling, idempotency, state machines, payment reconciliation, webhook reliability, audit trails, and transaction boundaries.

---

## Domain Ownership

```
Iced Latte owns:                          Stripe owns:
─────────────────────                     ──────────────────────
Order lifecycle                           Card/payment method collection
Cart snapshot at checkout time            3DS / SCA authentication
Pricing snapshot                          Payment authorization & capture
Delivery details                          Payment method compliance (PCI)
Payment state machine                     Provider-side webhooks
Refund state machine                      Hosted checkout UI
Idempotency (API + webhook)
Webhook processing & verification
Audit trail (order status history)
Email notifications
Admin / order support flows
```

**Key principle:** Stripe references your order (via `client_reference_id` and `metadata`), but Stripe does not define your order.

---

## Current State Audit

### Backend — What Exists

| Component | File | Status |
|-----------|------|--------|
| `StripeSessionCreator` | `payment/api/StripeSessionCreator.java` | Returns `clientSecret` — wrong for hosted checkout |
| `StripeWebhookService` | `payment/api/StripeWebhookService.java` | Handles `completed`, `expired`, `charge.refunded` — good base |
| `PaymentEndpoint` | `payment/endpoint/PaymentEndpoint.java` | `POST /payment` + `GET /payment/order` + webhook |
| `StripeSessionLineItemListConverter` | `payment/converter/...` | MapStruct cart→Stripe line items — reusable |
| `OrderCreator` | `order/api/OrderCreator.java` | Two paths: `create()` (direct) and `createOrderAndDeleteCart(Session)` (Stripe) |
| `OrderStatusTransitioner` | `order/api/OrderStatusTransitioner.java` | State machine: `CREATED→PAID→SHIPPED→DELIVERED`, cancel, refund |
| `Order` entity | `order/entity/Order.java` | Has `stripePaymentIntentId` column — never populated |
| Stripe config | `application.yaml` | `stripe.enabled=false`, secret-key, webhook-secret |
| Dependency | `pom.xml` | `stripe-java:32.0.0` |
| Tests | 2 files | Signature validation + email confirmation only |

### Backend — Critical Bugs

| # | Bug | Impact |
|---|-----|--------|
| 1 | `StripeSessionCreator` returns `session.getClientSecret()` without setting `ui_mode` | Client secret is null for default hosted mode |
| 2 | `createOrderAndDeleteCart(Session)` reads the **live cart** at webhook time | Cart mutation between checkout and webhook → wrong items ordered |
| 3 | `processRedirect()` also calls `handleCompleted()` — dual order creation | Race condition: webhook + redirect both try to create the order |
| 4 | `stripePaymentIntentId` never set on Order | `handleChargeRefunded()` searches by this field — refunds silently fail |
| 5 | `buildReturnUrl()` uses `request.getScheme()` + `Host` header | Breaks behind reverse proxies (Nginx, CloudFront) |
| 6 | No Stripe idempotency keys on `Session.create()` | Retries create duplicate Stripe sessions |
| 7 | No webhook event deduplication | Duplicate webhooks could create duplicate orders |

### Frontend — What Exists

| Component | File | Status |
|-----------|------|--------|
| Checkout form | `features/checkout/hooks/useCheckoutForm.ts` | Calls `createOrder()` directly — no payment step |
| Order success (legacy) | `features/orders/components/OrderSuccess.tsx` | Calls `GET /payment/order?sessionId=...` — disconnected |
| Cart store | `features/cart/state/cartStore.ts` | Zustand, dual-mode (guest/auth) |
| Orders API | `features/orders/api/ordersApi.ts` | Full CRUD with idempotency key support |
| API proxy | `app/api/proxy/[...path]/route.ts` | Proxies all calls with auth cookie forwarding |
| Stripe SDK | — | **Not installed** — zero `@stripe/*` packages |

### Frontend — Issues

| # | Issue | Impact |
|---|-------|--------|
| 1 | `useCheckoutForm` calls `POST /orders` directly | Orders created without payment |
| 2 | `resetCart()` called immediately after order creation | Cart cleared before payment confirmation |
| 3 | `OrderSuccess.tsx` orphaned from active checkout flow | Dead code |

---

## Target Architecture

### The Key Design Change

Instead of the previous plan's approach:

```
Cart → Stripe Session → Webhook → Create Order
```

This plan uses:

```
Cart → Create local Order(PENDING_PAYMENT) → Stripe Session → Webhook → Mark Order(PAID)
```

The order exists before payment. It is not a completed order — it is a pending order. The webhook transitions it to `PAID`.

### Payment Flow

```
┌──────────┐     ┌──────────────────────┐     ┌────────────────┐     ┌──────────────────────┐
│ Frontend  │     │       Backend        │     │     Stripe     │     │    Backend Webhook   │
│ Checkout  │     │   Payment Service    │     │  Hosted Page   │     │      Handler         │
└─────┬─────┘     └──────────┬───────────┘     └───────┬────────┘     └──────────┬───────────┘
      │                      │                         │                         │
      │ 1. POST /api/v1/payment/checkout               │                         │
      │    {recipient, address}                        │                         │
      ├─────────────────────►│                         │                         │
      │                      │                         │                         │
      │                      │ 2. Validate cart,       │                         │
      │                      │    address, user,       │                         │
      │                      │    price, stock         │                         │
      │                      │                         │                         │
      │                      │ 3. Create Order         │                         │
      │                      │    (PENDING_PAYMENT)    │                         │
      │                      │    with cart snapshot    │                         │
      │                      │                         │                         │
      │                      │ 4. Create Payment       │                         │
      │                      │    (CREATED)            │                         │
      │                      │                         │                         │
      │                      │ 5. Session.create()     │                         │
      │                      │    idempotency key      │                         │
      │                      │    metadata: orderId    │                         │
      │                      ├────────────────────────►│                         │
      │                      │                         │                         │
      │                      │◄────────────────────────┤                         │
      │                      │  {id, url}              │                         │
      │                      │                         │                         │
      │                      │ 6. Update Payment       │                         │
      │                      │    (STRIPE_SESSION_CREATED)                       │
      │                      │    store stripeSessionId │                         │
      │                      │                         │                         │
      │◄─────────────────────┤                         │                         │
      │ 7. {orderId, stripeSessionId, checkoutUrl}     │                         │
      │                      │                         │                         │
      │ 8. window.location.href = checkoutUrl          │                         │
      ├───────────────────────────────────────────────►│                         │
      │                      │                         │                         │
      │                      │                         │ 9. User pays            │
      │                      │                         │                         │
      │                      │                         │ 10. checkout.session.completed
      │                      │                         ├────────────────────────►│
      │                      │                         │                         │
      │                      │                         │  11. Check stripe_webhook_events
      │                      │                         │      (deduplicate)      │
      │                      │                         │                         │
      │                      │                         │  12. Find Order by orderId
      │                      │                         │      from metadata      │
      │                      │                         │                         │
      │                      │                         │  13. If already PAID →  │
      │                      │                         │      return 200         │
      │                      │                         │                         │
      │                      │                         │  14. Store stripePaymentIntentId
      │                      │                         │      Mark Payment PAID  │
      │                      │                         │      Mark Order PAID    │
      │                      │                         │      Delete cart        │
      │                      │                         │      Send email         │
      │                      │                         │                         │
      │                      │                         │◄────────────────────────┤
      │                      │                         │  200 OK                 │
      │                      │                         │                         │
      │◄──────────────────────────────────────────────┤                         │
      │ 15. Redirect to /checkout/success?session_id=...                        │
      │                      │                         │                         │
      │ 16. GET /api/v1/payment/checkout/{orderId}/status                       │
      ├─────────────────────►│                         │                         │
      │                      │                         │                         │
      │◄─────────────────────┤                         │                         │
      │ {status: PAID, orderId: ...}                   │                         │
      │                      │                         │                         │
      │ 17. Show confirmation, resetCart()             │                         │
```

### Order Status State Machine (Updated)

```
                    ┌─────────────────┐
                    │ PENDING_PAYMENT │ ◄── Order created at checkout
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
     ┌────────────┐  ┌──────────────┐  ┌─────────────────┐
     │    PAID    │  │PAYMENT_FAILED│  │ PAYMENT_EXPIRED  │
     └──────┬─────┘  └──────────────┘  └─────────────────┘
            │
      ┌─────┼──────────┐
      │                │
      ▼                ▼
 ┌─────────┐     ┌───────────┐
 │ SHIPPED │     │ CANCELLED │
 └────┬────┘     └───────────┘
      │
      ▼
 ┌───────────┐
 │ DELIVERED │
 └───────────┘

 PAID ──► REFUND_REQUESTED ──► REFUNDED
```

**New statuses:** `PENDING_PAYMENT`, `PAYMENT_FAILED`, `PAYMENT_EXPIRED`
**New events:** `PENDING_PAYMENT_CONFIRMED`, `PAYMENT_FAILED_EVENT`, `PAYMENT_EXPIRED_EVENT`

**Updated transitions map:**

```java
Map<OrderStatus, Map<OrderEvent, OrderStatus>> TRANSITIONS = Map.of(
    PENDING_PAYMENT, Map.of(
        PENDING_PAYMENT_CONFIRMED, PAID,
        PAYMENT_FAILED_EVENT, PAYMENT_FAILED,
        PAYMENT_EXPIRED_EVENT, PAYMENT_EXPIRED,
        CANCEL, CANCELLED
    ),
    CREATED, Map.of(PAYMENT_CONFIRMED, PAID, CANCEL, CANCELLED),  // keep for non-Stripe
    PAID, Map.of(SHIP, SHIPPED, CANCEL, CANCELLED, REQUEST_REFUND, REFUND_REQUESTED),
    SHIPPED, Map.of(DELIVER, DELIVERED),
    REFUND_REQUESTED, Map.of(REFUND_CONFIRMED, REFUNDED)
);
```

### Design Decisions

1. **Order-first, not payment-first** — The order exists before Stripe is called. Cart is snapshotted into order items at creation time. No separate "payment session" entity holding cart data.

2. **Payment entity separate from Order** — `Payment` tracks provider-specific details (Stripe session ID, payment intent ID, provider status). Order tracks business status. Clean domain separation.

3. **StripeWebhookEvent for idempotency** — Dedicated table deduplicates webhook events by `stripe_event_id`. Process-then-insert pattern prevents duplicate processing.

4. **Webhook is the source of truth** — Only the webhook handler transitions `PENDING_PAYMENT → PAID`. The success page only reads status.

5. **Domain service separation** — Stripe service calls `OrderStatusTransitioner` and `PaymentService` — it does not directly mutate Order fields.

6. **Hosted Checkout** — No Stripe frontend SDK needed. Stripe hosts the payment page. Minimal PCI scope.

---

## Backend Changes

### Phase 1: Payment Entity

A new entity to track payment provider details, separate from the Order.

**File:** `payment/entity/Payment.java`

```java
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PaymentProvider provider;  // STRIPE

    @Column(name = "provider_session_id", unique = true)
    private String providerSessionId;

    @Column(name = "provider_payment_intent_id")
    private String providerPaymentIntentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
```

**Enums:**

```java
public enum PaymentProvider { STRIPE }

public enum PaymentStatus {
    CREATED,                  // Payment record created, Stripe not yet called
    STRIPE_SESSION_CREATED,   // Stripe session created, awaiting user payment
    PAID,                     // Stripe confirmed payment
    FAILED,                   // Stripe async payment failed
    EXPIRED,                  // Stripe session expired
    REFUNDED                  // Stripe refund confirmed
}
```

### Phase 2: StripeWebhookEvent Entity

Deduplicates webhook events. Prevents double-processing on Stripe retries.

**File:** `payment/entity/StripeWebhookEvent.java`

```java
@Entity
@Table(name = "stripe_webhook_events")
public class StripeWebhookEvent {

    @Id
    @Column(name = "stripe_event_id", nullable = false, unique = true)
    private String stripeEventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;
}
```

### Phase 3: Liquibase Migrations

**File:** `db/changelog/version-2.0/XX.XX.2026.part1.create-payments-table.sql`

```sql
CREATE TABLE IF NOT EXISTS public.payments (
    id                          UUID PRIMARY KEY,
    order_id                    UUID NOT NULL UNIQUE REFERENCES orders(id),
    provider                    VARCHAR(20) NOT NULL DEFAULT 'STRIPE',
    provider_session_id         VARCHAR(255) UNIQUE,
    provider_payment_intent_id  VARCHAR(255),
    status                      VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    amount                      DECIMAL NOT NULL,
    currency                    VARCHAR(3) NOT NULL DEFAULT 'usd',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    updated_at                  TIMESTAMPTZ
);

CREATE INDEX idx_payments_order_id ON public.payments (order_id);
CREATE INDEX idx_payments_provider_session_id ON public.payments (provider_session_id);
CREATE INDEX idx_payments_provider_payment_intent_id
    ON public.payments (provider_payment_intent_id)
    WHERE provider_payment_intent_id IS NOT NULL;
```

**File:** `db/changelog/version-2.0/XX.XX.2026.part2.create-stripe-webhook-events-table.sql`

```sql
CREATE TABLE IF NOT EXISTS public.stripe_webhook_events (
    stripe_event_id  VARCHAR(255) PRIMARY KEY,
    event_type       VARCHAR(100) NOT NULL,
    processed_at     TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);
```

Register both in `changelog-master-version-2.0.yaml`.

### Phase 4: OpenAPI Spec Updates

**File:** `api-specs/order-openapi.yaml` — add new statuses and events:

```yaml
OrderStatus:
  type: string
  enum:
    - PENDING_PAYMENT    # NEW
    - PAYMENT_FAILED     # NEW
    - PAYMENT_EXPIRED    # NEW
    - CREATED
    - PAID
    - SHIPPED
    - DELIVERED
    - CANCELLED
    - REFUND_REQUESTED
    - REFUNDED

OrderEvent:
  type: string
  enum:
    - PENDING_PAYMENT_CONFIRMED  # NEW: webhook confirms payment
    - PAYMENT_FAILED_EVENT       # NEW: async payment failed
    - PAYMENT_EXPIRED_EVENT      # NEW: session expired
    - PAYMENT_CONFIRMED
    - SHIP
    - DELIVER
    - CANCEL
    - REQUEST_REFUND
    - REFUND_CONFIRMED
```

**File:** `api-specs/payment-openapi.yaml` — replace entirely:

```yaml
paths:
  /api/v1/payment/checkout:
    post:
      tags: ["Payment"]
      summary: "Create order and Stripe Checkout Session"
      description: >
        Validates cart/address/user, creates a local Order (PENDING_PAYMENT),
        creates a Stripe Hosted Checkout Session, returns the redirect URL.
      operationId: "createCheckout"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/CreateCheckoutRequestDto"
      responses:
        "200":
          description: "Checkout session created"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/CheckoutResponseDto"
        "400":
          description: "Validation error (empty cart, invalid address)"
        "401":
          description: "Unauthorized"

  /api/v1/payment/checkout/{orderId}/status:
    get:
      tags: ["Payment"]
      summary: "Check payment status for an order"
      description: >
        Read-only endpoint for the success page. Returns current order/payment
        status. Does NOT create or modify anything.
      operationId: "getCheckoutStatus"
      parameters:
        - name: orderId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        "200":
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/CheckoutStatusDto"

  /api/v1/payment/stripe/webhook:
    post:
      tags: ["Payment"]
      summary: "Stripe webhook receiver"
      description: "Receives and processes Stripe webhook events. Signature verified."
      operationId: "processStripeWebhook"
      # No auth — Stripe calls this directly. Signature verification is the auth.

components:
  schemas:
    CreateCheckoutRequestDto:
      type: object
      required: [recipientName, recipientSurname]
      properties:
        recipientName:
          type: string
          minLength: 2
          maxLength: 128
        recipientSurname:
          type: string
          minLength: 2
          maxLength: 128
        recipientPhone:
          type: string
          maxLength: 32
        deliveryAddressId:
          type: string
          format: uuid
        address:
          $ref: "common-schemas.yaml#/components/schemas/AddressDto"

    CheckoutResponseDto:
      type: object
      required: [orderId, stripeSessionId, checkoutUrl]
      properties:
        orderId:
          type: string
          format: uuid
        stripeSessionId:
          type: string
        checkoutUrl:
          type: string
          format: uri

    CheckoutStatusDto:
      type: object
      required: [orderId, orderStatus]
      properties:
        orderId:
          type: string
          format: uuid
        orderStatus:
          $ref: "order-openapi.yaml#/components/schemas/OrderStatus"
        paymentStatus:
          type: string
          enum: [CREATED, STRIPE_SESSION_CREATED, PAID, FAILED, EXPIRED, REFUNDED]
```

### Phase 5: Update OrderStatusTransitioner

Add new transitions for the payment flow. Keep existing transitions for backward compatibility.

**File:** `order/api/OrderStatusTransitioner.java`

```java
private static final Map<OrderStatus, Map<OrderEvent, OrderStatus>> TRANSITIONS = Map.of(
    // NEW: Stripe payment flow
    PENDING_PAYMENT, Map.of(
        PENDING_PAYMENT_CONFIRMED, PAID,
        PAYMENT_FAILED_EVENT, PAYMENT_FAILED,
        PAYMENT_EXPIRED_EVENT, PAYMENT_EXPIRED,
        CANCEL, CANCELLED
    ),
    // EXISTING: non-Stripe flow (keep for stripe.enabled=false)
    CREATED, Map.of(PAYMENT_CONFIRMED, PAID, CANCEL, CANCELLED),
    // EXISTING: post-payment flows
    PAID, Map.of(SHIP, SHIPPED, CANCEL, CANCELLED, REQUEST_REFUND, REFUND_REQUESTED),
    SHIPPED, Map.of(DELIVER, DELIVERED),
    REFUND_REQUESTED, Map.of(REFUND_CONFIRMED, REFUNDED)
);
```

No other changes to `OrderStatusTransitioner` — the existing `transition()` method, guard validation, and event publishing all work as-is.

### Phase 6: CheckoutPaymentService (Core Flow)

New service that orchestrates the checkout: validate → create order → create payment → call Stripe → return URL.

**File:** `payment/api/CheckoutPaymentService.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class CheckoutPaymentService {

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final ShoppingCartService shoppingCartService;
    private final OrderCreator orderCreator;
    private final PaymentRepository paymentRepository;
    private final StripeCheckoutSessionCreator stripeSessionCreator;

    @Transactional
    public CheckoutResponseDto checkout(CreateCheckoutRequestDto request) {
        UserDto user = securityPrincipalProvider.get();
        UUID userId = user.getId();

        // 1. Validate cart
        ShoppingCartDto cart = shoppingCartService.getByUserIdOrThrow(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot checkout: shopping cart is empty");
        }

        // 2. Create Order(PENDING_PAYMENT) — snapshots cart items into order items
        Order order = orderCreator.createPendingPaymentOrder(userId, request, cart);

        // 3. Create Payment record
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .provider(PaymentProvider.STRIPE)
                .status(PaymentStatus.CREATED)
                .amount(order.getItemsTotalPrice())
                .currency("usd")
                .createdAt(OffsetDateTime.now())
                .build();
        payment = paymentRepository.save(payment);

        // 4. Create Stripe Checkout Session
        StripeSessionResult stripeResult = stripeSessionCreator.create(
                order, user.getEmail(), cart.getItems());

        // 5. Update Payment with Stripe details
        payment.setProviderSessionId(stripeResult.sessionId());
        payment.setStatus(PaymentStatus.STRIPE_SESSION_CREATED);
        payment.setUpdatedAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        // 6. Store Stripe session ID on Order for lookup
        order.setSessionId(stripeResult.sessionId());
        // order is already managed by JPA — will flush at transaction commit

        log.info("checkout.created: orderId={}, stripeSessionId={}",
                order.getId(), mask(stripeResult.sessionId()));

        return new CheckoutResponseDto()
                .orderId(order.getId())
                .stripeSessionId(stripeResult.sessionId())
                .checkoutUrl(stripeResult.checkoutUrl());
    }
}
```

### Phase 7: Update OrderCreator

Add a new method for creating pending-payment orders. Keep the existing `create()` for non-Stripe flows.

**File:** `order/api/OrderCreator.java` — add method:

```java
@Transactional
public Order createPendingPaymentOrder(UUID userId, CreateCheckoutRequestDto request,
                                       ShoppingCartDto cart) {
    // Reuse existing address resolution logic
    Address deliveryAddress = resolveAddress(request, userId);

    // Snapshot cart items into order items (same as existing create())
    List<OrderItem> items = cart.getItems().stream()
            .map(orderDtoConverter::toOrderItem)
            .toList();

    Order order = Order.builder()
            .userId(userId)
            .sessionId(UUID.randomUUID().toString())  // placeholder, updated after Stripe call
            .status(OrderStatus.PENDING_PAYMENT)       // KEY CHANGE
            .items(items)
            .deliveryAddress(deliveryAddress)
            .recipientName(request.getRecipientName())
            .recipientSurname(request.getRecipientSurname())
            .recipientPhone(request.getRecipientPhone())
            .itemsQuantity(cart.getItemsQuantity())
            .itemsTotalPrice(cart.getItemsTotalPrice())
            .cancellationDeadline(OffsetDateTime.now().plusMinutes(cancellationWindowMinutes))
            .build();

    return orderRepository.save(order);
}
```

The `resolveAddress()` private method already handles both `deliveryAddressId` and inline `address` — reuse it by extracting to a shared helper or accepting the same field names in `CreateCheckoutRequestDto`.

**Remove:** `createOrderAndDeleteCart(Session stripeSession)` — this method reads the live cart at webhook time and is the source of the cart mutation bug. It is fully replaced by the order-first flow.

### Phase 8: Rewrite StripeCheckoutSessionCreator

Renamed from `StripeSessionCreator`. Now receives an Order (not HttpServletRequest), uses `FRONTEND_URL`, and adds idempotency keys.

**File:** `payment/api/StripeCheckoutSessionCreator.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripeCheckoutSessionCreator {

    private final StripeSessionLineItemListConverter lineItemConverter;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${frontend.url}")
    private String frontendUrl;

    @PostConstruct
    private void initStripe() {
        Stripe.apiKey = stripeSecretKey;
    }

    public StripeSessionResult create(Order order, String customerEmail,
                                      List<ShoppingCartItemDto> cartItems) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(customerEmail)
                .setSuccessUrl(frontendUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/checkout/cancel?order_id=" + order.getId())
                .setClientReferenceId(order.getId().toString())
                .putMetadata("orderId", order.getId().toString())
                .putMetadata("userId", order.getUserId().toString())
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("orderId", order.getId().toString())
                                .putMetadata("userId", order.getUserId().toString())
                                .build())
                .addAllLineItem(lineItemConverter.toLineItems(cartItems))
                .addAllShippingOption(shippingOptions())
                .setExpiresAt(OffsetDateTime.now().plusMinutes(30).toEpochSecond())
                .build();

        // Idempotency key tied to orderId — retries reuse the same Stripe session
        RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey("checkout-session:" + order.getId())
                .build();

        try {
            Session session = Session.create(params, requestOptions);
            return new StripeSessionResult(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new StripeSessionCreationException(e.getMessage(), e);
        }
    }

    // shippingOptions() — unchanged from current implementation
}
```

**Record:** `payment/api/StripeSessionResult.java`

```java
public record StripeSessionResult(String sessionId, String checkoutUrl) {}
```

**Key changes from current `StripeSessionCreator`:**
- Returns `session.getUrl()` instead of `session.getClientSecret()`
- Uses `frontendUrl` config instead of `buildReturnUrl(request)`
- Adds `RequestOptions` with idempotency key
- Sets `client_reference_id` and `metadata.orderId` for webhook lookup
- Sets `payment_intent_data.metadata` so refund webhooks can also find the order
- Sets `expires_at` (30 min) to match business logic

### Phase 9: Rewrite StripeWebhookService

The webhook handler now transitions existing orders instead of creating them.

**File:** `payment/api/StripeWebhookService.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripeWebhookService {

    private final OrderStatusTransitioner orderStatusTransitioner;
    private final PaymentRepository paymentRepository;
    private final StripeWebhookEventRepository webhookEventRepository;
    private final OrderRepository orderRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final PaymentEmailConfirmation paymentEmailConfirmation;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Transactional
    public void processWebhook(String payload, String stripeSignature) {
        Event event = parseEvent(payload, stripeSignature);

        // Idempotency: skip already-processed events
        if (webhookEventRepository.existsById(event.getId())) {
            log.info("payment.webhook.duplicate: eventId={}", event.getId());
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCompleted(requireSession(event));
            case "checkout.session.expired" -> handleExpired(requireSession(event));
            case "checkout.session.async_payment_succeeded" -> handleCompleted(requireSession(event));
            case "checkout.session.async_payment_failed" -> handleAsyncPaymentFailed(requireSession(event));
            case "charge.refunded" -> handleChargeRefunded(event);
            default -> log.debug("payment.webhook.unhandled: eventType={}", event.getType());
        }

        // Record processed event
        webhookEventRepository.save(new StripeWebhookEvent(
                event.getId(), event.getType(), OffsetDateTime.now()));

        log.info("payment.webhook.processed: eventType={}, eventId={}", event.getType(), event.getId());
    }

    private void handleCompleted(Session stripeSession) {
        UUID orderId = extractOrderId(stripeSession);

        // Find payment
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("No Payment for orderId=" + orderId));

        // Already paid — idempotent
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("payment.already_paid: orderId={}", orderId);
            return;
        }

        // Update Payment
        payment.setProviderPaymentIntentId(stripeSession.getPaymentIntent());
        payment.setStatus(PaymentStatus.PAID);
        payment.setUpdatedAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        // Update Order: PENDING_PAYMENT → PAID
        Order order = orderStatusTransitioner.transition(
                orderId, OrderEvent.PENDING_PAYMENT_CONFIRMED, null, "Stripe payment confirmed");

        // Store stripePaymentIntentId on Order (for refund lookup)
        order.setStripePaymentIntentId(stripeSession.getPaymentIntent());
        orderRepository.save(order);

        // Delete cart
        shoppingCartRepository.deleteByUserId(order.getUserId());

        // Send confirmation email
        paymentEmailConfirmation.send(stripeSession);

        log.info("checkout.completed: orderId={}, paymentIntentId={}",
                orderId, stripeSession.getPaymentIntent());
    }

    private void handleExpired(Session stripeSession) {
        UUID orderId = extractOrderId(stripeSession);

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.EXPIRED);
            payment.setUpdatedAt(OffsetDateTime.now());
            paymentRepository.save(payment);
        });

        try {
            orderStatusTransitioner.transition(
                    orderId, OrderEvent.PAYMENT_EXPIRED_EVENT, null, "Stripe session expired");
        } catch (InvalidOrderStateTransitionException e) {
            log.warn("order.expire.transition_failed: orderId={}", orderId);
        }
    }

    private void handleAsyncPaymentFailed(Session stripeSession) {
        UUID orderId = extractOrderId(stripeSession);

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(OffsetDateTime.now());
            paymentRepository.save(payment);
        });

        try {
            orderStatusTransitioner.transition(
                    orderId, OrderEvent.PAYMENT_FAILED_EVENT, null, "Stripe async payment failed");
        } catch (InvalidOrderStateTransitionException e) {
            log.warn("order.payment_failed.transition_failed: orderId={}", orderId);
        }
    }

    private void handleChargeRefunded(Event event) {
        // Unchanged from current implementation — already correct.
        // Searches by stripePaymentIntentId (now populated in handleCompleted).
    }

    private UUID extractOrderId(Session session) {
        String orderId = session.getClientReferenceId();
        if (orderId == null) {
            orderId = session.getMetadata().get("orderId");
        }
        if (orderId == null) {
            throw new IllegalStateException("No orderId in Stripe session metadata");
        }
        return UUID.fromString(orderId);
    }

    // parseEvent(), requireSession() — unchanged from current implementation
}
```

**Key changes from current `StripeWebhookService`:**
- Webhook event deduplication via `StripeWebhookEvent` table
- `handleCompleted()` transitions existing order (`PENDING_PAYMENT → PAID`) instead of creating one
- Sets `stripePaymentIntentId` on Order (fixes refund lookup)
- Handles `async_payment_succeeded` and `async_payment_failed` for delayed payment methods
- `processRedirect()` removed entirely — success page uses a read-only status endpoint

### Phase 10: Update PaymentEndpoint

**File:** `payment/endpoint/PaymentEndpoint.java`

```java
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiPaths.PAYMENT)
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class PaymentEndpoint implements PaymentApi {

    private final CheckoutPaymentService checkoutPaymentService;
    private final PaymentStatusService paymentStatusService;
    private final StripeWebhookService stripeWebhookService;

    @Override
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponseDto> createCheckout(
            @Valid @RequestBody CreateCheckoutRequestDto request) {
        CheckoutResponseDto response = checkoutPaymentService.checkout(request);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/checkout/{orderId}/status")
    public ResponseEntity<CheckoutStatusDto> getCheckoutStatus(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentStatusService.getStatus(orderId));
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> processStripeWebhook(
            @RequestHeader("Stripe-Signature") String stripeSignature,
            @RequestBody String payload) {
        stripeWebhookService.processWebhook(payload, stripeSignature);
        return ResponseEntity.ok().build();
    }
}
```

**Remove:** `processPayment()` (old `POST /payment`) and `processRedirectEvent()` (old `GET /payment/order`).

### Phase 11: PaymentStatusService (Read-Only)

For the success page to poll. Does not create or modify anything.

**File:** `payment/api/PaymentStatusService.java`

```java
@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public CheckoutStatusDto getStatus(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        CheckoutStatusDto dto = new CheckoutStatusDto()
                .orderId(order.getId())
                .orderStatus(order.getStatus());

        paymentRepository.findByOrderId(orderId)
                .ifPresent(p -> dto.paymentStatus(p.getStatus()));

        return dto;
    }
}
```

### Phase 12: Configuration

**File:** `application.yaml` — add:

```yaml
frontend:
  url: ${FRONTEND_URL:http://localhost:3000}
```

**File:** `.env.example` — add:

```bash
# Frontend URL (required for Stripe success/cancel redirects)
FRONTEND_URL=http://localhost:3000
```

Existing Stripe config unchanged:

```bash
STRIPE_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

---

## Frontend Changes

### Phase 1: New Payment API Module

No Stripe SDK needed for hosted checkout. Only Axios calls through the existing proxy.

**File:** `src/features/payment/api/paymentApi.ts`

```ts
import { api } from '@/shared/api/client'

export interface CreateCheckoutRequest {
  recipientName: string
  recipientSurname: string
  recipientPhone?: string
  deliveryAddressId?: string
  address?: { country: string; city: string; line: string; postcode: string }
}

export interface CheckoutResponse {
  orderId: string
  stripeSessionId: string
  checkoutUrl: string
}

export interface CheckoutStatus {
  orderId: string
  orderStatus: string
  paymentStatus?: string
}

export async function createCheckout(
  payload: CreateCheckoutRequest,
): Promise<CheckoutResponse> {
  const { data } = await api.post<CheckoutResponse>('/payment/checkout', payload)
  return data
}

export async function getCheckoutStatus(
  orderId: string,
  signal?: AbortSignal,
): Promise<CheckoutStatus> {
  const { data } = await api.get<CheckoutStatus>(
    `/payment/checkout/${orderId}/status`,
    { signal },
  )
  return data
}
```

**File:** `src/features/payment/public.ts`

```ts
export { createCheckout, getCheckoutStatus } from './api/paymentApi'
export type { CreateCheckoutRequest, CheckoutResponse, CheckoutStatus } from './api/paymentApi'
```

### Phase 2: Update useCheckoutForm Hook

**File:** `src/features/checkout/hooks/useCheckoutForm.ts`

Replace the `createOrder()` call with `createCheckout()` + redirect:

```ts
// BEFORE (current):
const order = await createOrder(payload, idempotencyKey)
resetCart()
router.push('/orders')

// AFTER:
const checkout = await createCheckout({
  recipientName: form.recipientName,
  recipientSurname: form.recipientSurname,
  recipientPhone: form.recipientPhone || undefined,
  ...(selectedAddress
    ? { deliveryAddressId: selectedAddress.id }
    : {
        address: {
          country: form.country,
          city: form.city,
          line: form.line,
          postcode: form.postcode,
        },
      }),
})

// Redirect to Stripe hosted checkout — full page navigation
window.location.href = checkout.checkoutUrl

// DO NOT resetCart() here.
// Backend deletes the server-side cart after payment confirmation.
// Frontend resets local cart state on the success page.
```

### Phase 3: New Checkout Success Page

**File:** `src/app/checkout/success/page.tsx`

```tsx
import { requireRecoverableSession } from '@/shared/auth/guards'
import { redirect } from 'next/navigation'
import { CheckoutSuccess } from '@/features/payment/components/CheckoutSuccess'

export default async function CheckoutSuccessPage({
  searchParams,
}: {
  searchParams: Promise<{ session_id?: string; order_id?: string }>
}) {
  await requireRecoverableSession('/checkout')
  const { session_id, order_id } = await searchParams
  if (!order_id && !session_id) redirect('/orders')
  return <CheckoutSuccess orderId={order_id} />
}
```

**File:** `src/features/payment/components/CheckoutSuccess.tsx`

```tsx
'use client'

import { useEffect, useState } from 'react'
import { getCheckoutStatus, type CheckoutStatus } from '../public'
import { useCartStore } from '@/features/cart/state/cartStore'
import Link from 'next/link'

export function CheckoutSuccess({ orderId }: { orderId?: string }) {
  const [status, setStatus] = useState<CheckoutStatus | null>(null)
  const [error, setError] = useState(false)
  const resetCart = useCartStore((s) => s.resetCart)

  useEffect(() => {
    if (!orderId) return
    const controller = new AbortController()
    let retries = 0

    async function poll() {
      try {
        const result = await getCheckoutStatus(orderId!, controller.signal)
        setStatus(result)
        if (result.orderStatus === 'PAID') {
          resetCart()
        } else if (result.orderStatus === 'PENDING_PAYMENT' && retries < 5) {
          retries++
          setTimeout(poll, 2000)
        }
      } catch {
        if (!controller.signal.aborted) setError(true)
      }
    }

    poll()
    return () => controller.abort()
  }, [orderId, resetCart])

  if (error) {
    return (
      <div>
        <h1>Something went wrong</h1>
        <p>We couldn't verify your payment. Check your <Link href="/orders">orders</Link>.</p>
      </div>
    )
  }

  if (!status || status.orderStatus === 'PENDING_PAYMENT') {
    return <div>Confirming your payment...</div>
  }

  if (status.orderStatus === 'PAID') {
    return (
      <div>
        <h1>Payment successful!</h1>
        <p>Your order has been placed.</p>
        <Link href="/orders">View your orders</Link>
      </div>
    )
  }

  return (
    <div>
      <h1>Payment not completed</h1>
      <p><Link href="/cart">Return to cart</Link> and try again.</p>
    </div>
  )
}
```

### Phase 4: New Checkout Cancel Page

When the user clicks "Back" on Stripe's hosted page, they return here.

**File:** `src/app/checkout/cancel/page.tsx`

```tsx
import { requireRecoverableSession } from '@/shared/auth/guards'
import Link from 'next/link'

export default async function CheckoutCancelPage() {
  await requireRecoverableSession('/checkout')
  return (
    <div>
      <h1>Payment cancelled</h1>
      <p>Your cart is still intact. <Link href="/checkout">Return to checkout</Link> or <Link href="/cart">edit your cart</Link>.</p>
    </div>
  )
}
```

### Phase 5: Remove Legacy Payment Code

**Delete:**
- `src/app/orders/success/page.tsx` — old Stripe redirect handler
- `src/features/orders/components/OrderSuccess.tsx` — old verification component

These are replaced by `/checkout/success` and `CheckoutSuccess`.

### Phase 6: Cart Behavior Changes

| Scenario | Before | After |
|----------|--------|-------|
| Checkout submit | `resetCart()` immediately | No cart change — redirect to Stripe |
| Payment confirmed | N/A | Backend deletes server cart; FE `resetCart()` on success page |
| Payment cancelled | N/A | Cart untouched — user can retry |
| Payment expired | N/A | Cart untouched — user can retry |
| User modifies cart after starting checkout | Corrupts order | No effect — order items were snapshotted |

---

## Testing Plan

### Backend Unit Tests

| Test Class | Test Cases |
|------------|------------|
| `CheckoutPaymentServiceTest` | Creates Order(PENDING_PAYMENT) + Payment(CREATED) + Stripe session |
| | Rejects empty cart |
| | Validates address (either `deliveryAddressId` or inline, not both) |
| | Snapshots cart items into order items at creation time |
| | Uses Stripe idempotency key `checkout-session:{orderId}` |
| `StripeWebhookServiceTest` | `checkout.session.completed` transitions `PENDING_PAYMENT → PAID` |
| | Duplicate `checkout.session.completed` is idempotent (no double transition) |
| | Duplicate Stripe event ID is skipped entirely |
| | `checkout.session.expired` transitions `PENDING_PAYMENT → PAYMENT_EXPIRED` |
| | `async_payment_succeeded` transitions `PENDING_PAYMENT → PAID` |
| | `async_payment_failed` transitions `PENDING_PAYMENT → PAYMENT_FAILED` |
| | `charge.refunded` finds order by `stripePaymentIntentId` |
| | Invalid `Stripe-Signature` returns 400 with safe error message |
| | Missing order for unknown `orderId` in metadata logs warning |
| | Sets `stripePaymentIntentId` on Order after payment |
| | Deletes cart after successful payment |
| `PaymentStatusServiceTest` | Returns `PAID` + order status for completed payments |
| | Returns `PENDING_PAYMENT` for in-progress orders |
| | Returns `PAYMENT_EXPIRED` for timed-out sessions |
| `OrderStatusTransitionerTest` | `PENDING_PAYMENT` + `PENDING_PAYMENT_CONFIRMED` → `PAID` |
| | `PENDING_PAYMENT` + `PAYMENT_FAILED_EVENT` → `PAYMENT_FAILED` |
| | `PENDING_PAYMENT` + `PAYMENT_EXPIRED_EVENT` → `PAYMENT_EXPIRED` |
| | `PENDING_PAYMENT` + `CANCEL` → `CANCELLED` |
| | Existing transitions unchanged |

### Backend Integration Tests

| Test | Description |
|------|-------------|
| `PaymentCheckoutIntegrationTest` | Full flow: checkout → mock webhook → verify order PAID with correct items |
| | Verify `stripePaymentIntentId` populated on order |
| | Verify cart deleted after webhook |
| | Verify Payment entity status is PAID |
| | Duplicate webhook is idempotent |
| | Expired session marks order PAYMENT_EXPIRED |

### Frontend Unit Tests

| Test | Description |
|------|-------------|
| `useCheckoutForm` | Calls `createCheckout` instead of `createOrder` |
| | Sets `window.location.href` to `checkoutUrl` |
| | Does NOT call `resetCart()` before redirect |
| `CheckoutSuccess` | Polls `getCheckoutStatus` on mount |
| | Calls `resetCart()` when orderStatus is `PAID` |
| | Shows loading state while `PENDING_PAYMENT` |
| | Shows error state on failure |
| | Retries up to 5 times for pending status |

### E2E Tests (Playwright)

| Test | Description |
|------|-------------|
| `checkout.spec.ts` | User fills form → mock backend returns checkoutUrl → verify redirect |
| `checkout-success.spec.ts` | Navigate to `/checkout/success?order_id=...` → mock status PAID → verify confirmation |
| `checkout-cancel.spec.ts` | Navigate to `/checkout/cancel` → verify cart intact message |

---

## File Change Summary

### Backend — New Files

| File | Description |
|------|-------------|
| `payment/entity/Payment.java` | Payment provider tracking (Stripe session, intent, status) |
| `payment/entity/PaymentProvider.java` | Enum: `STRIPE` |
| `payment/entity/PaymentStatus.java` | Enum: `CREATED`, `STRIPE_SESSION_CREATED`, `PAID`, `FAILED`, `EXPIRED`, `REFUNDED` |
| `payment/entity/StripeWebhookEvent.java` | Webhook event deduplication |
| `payment/repository/PaymentRepository.java` | JPA repository: `findByOrderId()`, `findByProviderSessionId()` |
| `payment/repository/StripeWebhookEventRepository.java` | JPA repository: `existsById()` |
| `payment/api/CheckoutPaymentService.java` | Orchestrates: validate → create order → create payment → call Stripe |
| `payment/api/StripeCheckoutSessionCreator.java` | Creates Stripe Hosted Checkout Session with idempotency |
| `payment/api/StripeSessionResult.java` | Record: `sessionId`, `checkoutUrl` |
| `payment/api/PaymentStatusService.java` | Read-only status for success page polling |
| `db/.../create-payments-table.sql` | Liquibase migration |
| `db/.../create-stripe-webhook-events-table.sql` | Liquibase migration |

### Backend — Modified Files

| File | Changes |
|------|---------|
| `order/api/OrderCreator.java` | Add `createPendingPaymentOrder()`. Remove `createOrderAndDeleteCart()`. |
| `order/api/OrderStatusTransitioner.java` | Add `PENDING_PAYMENT` transitions in `TRANSITIONS` map |
| `payment/endpoint/PaymentEndpoint.java` | New `POST /checkout`, `GET /checkout/{orderId}/status`. Remove old endpoints. |
| `payment/api/StripeWebhookService.java` | Transition orders instead of creating them. Add event deduplication. Handle async events. Remove `processRedirect()`. |
| `api-specs/payment-openapi.yaml` | New DTOs and endpoints |
| `api-specs/order-openapi.yaml` | Add `PENDING_PAYMENT`, `PAYMENT_FAILED`, `PAYMENT_EXPIRED` to OrderStatus. Add new OrderEvents. |
| `application.yaml` | Add `frontend.url` property |
| `.env.example` | Add `FRONTEND_URL` |

### Backend — Deleted

| Item | Reason |
|------|--------|
| `StripeSessionCreator.java` | Replaced by `StripeCheckoutSessionCreator.java` |
| `SessionWithClientSecretDto` (generated) | Replaced by `CheckoutResponseDto` |
| `OrderCreator.createOrderAndDeleteCart()` | Replaced by order-first flow |
| `StripeWebhookService.processRedirect()` | Replaced by read-only `PaymentStatusService` |
| `GET /api/v1/payment/order` endpoint | Replaced by `GET /checkout/{orderId}/status` |
| `POST /api/v1/payment` endpoint | Replaced by `POST /checkout` |

### Frontend — New Files

| File | Description |
|------|-------------|
| `features/payment/api/paymentApi.ts` | `createCheckout()`, `getCheckoutStatus()` |
| `features/payment/public.ts` | Barrel exports |
| `features/payment/components/CheckoutSuccess.tsx` | Success page with status polling |
| `app/checkout/success/page.tsx` | Route for Stripe success redirect |
| `app/checkout/cancel/page.tsx` | Route for Stripe cancel redirect |

### Frontend — Modified Files

| File | Changes |
|------|---------|
| `features/checkout/hooks/useCheckoutForm.ts` | Call `createCheckout()` + `window.location.href` instead of `createOrder()` + `router.push` |

### Frontend — Deleted

| File | Reason |
|------|--------|
| `app/orders/success/page.tsx` | Replaced by `/checkout/success` |
| `features/orders/components/OrderSuccess.tsx` | Replaced by `CheckoutSuccess.tsx` |

---

## Migration Strategy

### Step-by-Step Rollout

| Step | Branch | Description |
|------|--------|-------------|
| 1 | `feature/payment-entity` | Add `Payment`, `PaymentStatus`, `PaymentProvider`, `StripeWebhookEvent` entities + repositories + Liquibase migrations. No behavior changes. |
| 2 | `feature/order-pending-payment` | Add `PENDING_PAYMENT`, `PAYMENT_FAILED`, `PAYMENT_EXPIRED` to OrderStatus enum. Add new OrderEvents. Update `OrderStatusTransitioner` transitions map. Add `createPendingPaymentOrder()` to `OrderCreator`. |
| 3 | `feature/stripe-checkout-flow` | Add `CheckoutPaymentService`, `StripeCheckoutSessionCreator`, `PaymentStatusService`. Update `PaymentEndpoint` with new routes. Keep old routes temporarily. |
| 4 | `feature/webhook-order-transition` | Rewrite `StripeWebhookService` to transition orders + update payments. Add webhook event deduplication. Remove `processRedirect()`. |
| 5 | `feature/fe-stripe-checkout` | Add `features/payment/` module. Update `useCheckoutForm`. Add `/checkout/success` and `/checkout/cancel` pages. |
| 6 | `feature/cleanup-legacy-payment` | Remove old `StripeSessionCreator`, `SessionWithClientSecretDto`, `createOrderAndDeleteCart()`, old endpoints, old `OrderSuccess.tsx`. |
| 7 | `feature/payment-tests` | Add unit tests, integration tests, E2E tests. |

### Backward Compatibility

- The direct `POST /orders` endpoint remains for non-Stripe flows (when `stripe.enabled=false`)
- Feature flag `stripe.enabled` controls whether payment endpoints are registered
- Old endpoints can coexist during migration (steps 3-4) before cleanup (step 6)

---

## Priority Order

| # | Task | Risk if Skipped |
|---|------|-----------------|
| 1 | Add `Payment` entity + `StripeWebhookEvent` entity + migrations | No payment tracking, no webhook deduplication |
| 2 | Add `PENDING_PAYMENT` status + transitions to `OrderStatusTransitioner` | Cannot create orders before payment |
| 3 | Build `CheckoutPaymentService` — order-first checkout flow | No Stripe integration |
| 4 | Build `StripeCheckoutSessionCreator` with idempotency keys | Duplicate Stripe sessions on retry |
| 5 | Rewrite `StripeWebhookService` — transition orders, deduplicate events | Duplicate orders, missing `stripePaymentIntentId`, broken refunds |
| 6 | Frontend: `useCheckoutForm` → `createCheckout()` + redirect | No payment collected |
| 7 | Frontend: `/checkout/success` page with status polling | No confirmation after payment |
| 8 | Frontend: `/checkout/cancel` page | User stranded after cancelling payment |
| 9 | Use `FRONTEND_URL` config for success/cancel URLs | Broken redirects behind proxies |
| 10 | Remove legacy code: old endpoints, `createOrderAndDeleteCart()`, `OrderSuccess.tsx` | Dead code, dual creation paths |
| 11 | Add tests (unit, integration, E2E) | Regressions on future changes |

---

## Local Development Setup

```bash
# 1. Set env vars
STRIPE_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...        # from Stripe Dashboard → Developers → API keys
STRIPE_WEBHOOK_SECRET=whsec_...      # from stripe listen output (step 3)
FRONTEND_URL=http://localhost:3000

# 2. Start backend
export $(cat .env | xargs) && mvn spring-boot:run

# 3. Start Stripe CLI webhook forwarding
stripe listen --forward-to localhost:8083/api/v1/payment/stripe/webhook

# 4. Start frontend
cd ../Iced-Latte-Frontend && npm run dev

# 5. Test the flow
# - Add items to cart
# - Go to checkout, fill form, submit
# - You'll be redirected to Stripe's test checkout page
# - Use test card: 4242 4242 4242 4242, any future expiry, any CVC
# - After payment, you'll be redirected to /checkout/success
# - The success page polls until the webhook confirms payment
```

---

## References

- [Stripe Checkout Sessions API](https://docs.stripe.com/api/checkout/sessions/create)
- [Stripe Checkout Fulfillment](https://docs.stripe.com/checkout/fulfillment) — automatic fulfillment, webhook events, idempotency
- [Stripe Webhooks & Signatures](https://docs.stripe.com/webhooks)
- [Stripe Idempotent Requests](https://docs.stripe.com/api/idempotent_requests)
- [Stripe CLI for Local Testing](https://docs.stripe.com/stripe-cli)
- [Stripe client_reference_id](https://docs.stripe.com/api/checkout/sessions) — reconcile sessions with internal systems

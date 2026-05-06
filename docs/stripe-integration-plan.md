# Stripe Integration Plan — Iced Latte

**Date:** 2026-05-04
**Scope:** Backend (Java 25 / Spring Boot 4) + Frontend (Next.js 16)
**Stripe Mode:** Hosted Checkout Sessions (redirect-based)
**Architecture:** Backend-owned order-first — Iced Latte owns the order lifecycle; Stripe only handles payment collection.
**Status:** Historical design archive

> ⚠️ **Archive notice:** this document records the Stripe implementation plan and design reasoning. It is not guaranteed to match the current implementation line by line. Before changing payment code, verify the current `payment/`, `order/`, OpenAPI specs, Liquibase migrations, frontend checkout flow, tests, and [Stripe Payment Integration — System Design Guide](stripe-payment-guide.md).

---

## Executive Summary

Iced Latte will integrate Stripe using a **backend-owned order-first** architecture. The backend creates a local Order with status `PENDING_PAYMENT` *before* creating the Stripe Checkout Session. Stripe collects the payment; the webhook transitions the order to `PAID`. The order, cart snapshot, pricing, delivery details, and payment state machine all live in Iced Latte's database — Stripe is an external payment rail, not the order system.

This design gives the project real senior-level backend engineering: domain modelling, idempotency, state machines, payment reconciliation, webhook reliability, audit trails, and transaction boundaries.

---

## Lean Implementation Constraints

### Project Context

Iced Latte is a **non-commercial, source-available pet project** created for learning and gaining senior software engineering experience. This Stripe integration uses **Stripe test mode / sandbox only** — no real cards, no real money. Stripe Hosted Checkout in test mode handles all card UI; Iced Latte never sees or stores card details.

### Test / Sandbox Payment Rules

- Use Stripe **test API keys** only (`sk_test_...`, `whsec_...`). Never commit live keys.
- Use [Stripe test cards](https://docs.stripe.com/testing#cards) (e.g., `4242 4242 4242 4242`). Do not use real card details.
- Frontend checkout UI should indicate: _"Test payment only — no real money is charged. Use Stripe test cards."_
- `.env.example` must contain test-mode placeholders only.

### Engineering Principles

| Principle | Rule |
|-----------|------|
| **SOLID** | Follow it, but do not over-abstract. Stripe is the only provider for this PR — no `AbstractPaymentProvider` or strategy pattern. |
| **KISS** | Prefer the simplest correct solution. If direct construction is clearer than a mapper/converter, use direct construction. |
| **YAGNI** | Do not build for hypothetical future providers, event sourcing, saga frameworks, workflow engines, or admin replay endpoints. |
| **High cohesion** | Payment logic stays in `payment/`. Order state transitions stay in `order/`. Do not leak feature logic into `common/`. |
| **Loose coupling** | Services communicate through method calls and Spring events, not shared mutable state. |

### File / Class Count Constraints

This is an **application feature**, not a payment framework or reusable library.

- **Minimize new files.** Do not create one tiny class per tiny responsibility.
- **Avoid micro-classes** that contain only one trivial method — unless Spring transaction boundaries require a separate bean (e.g., `REQUIRES_NEW` for webhook event recording).
- **Avoid files over ~300 lines.** If a class exceeds ~300 lines, split by real feature responsibility, not by artificial layers.
- **Prefer modifying existing files** when the responsibility already belongs there.
- **Combine tiny classes** that are always used together, unless transaction boundary separation is required.

**Acceptable reasons to create a new file:**
- JPA entity or repository is needed.
- OpenAPI spec update generates a new DTO.
- Spring transaction boundary requires a separate bean.
- A class would otherwise exceed ~300 lines.
- A responsibility is clearly separate and reused in more than one place.

### Naming Rules

Use **clear, boring, precise** names that describe business responsibility:

| ✅ Good | ❌ Avoid |
|---------|----------|
| `CheckoutPaymentService` | `PaymentOrchestrator` |
| `StripeCheckoutSessionCreator` | `PaymentEngine` |
| `StripeWebhookService` | `PaymentFramework` |
| `StripeWebhookEventRecorder` | `AbstractPaymentProvider` |
| `PaymentStatusService` | `PaymentProviderStrategy` |
| `createCheckout`, `markPaid`, `handleExpired` | `Manager`, `Processor`, `Handler`, `Util` (unless no better domain name exists) |

### Anti-Overengineering Checklist

Do **NOT** add any of the following in this PR:

- ❌ Strategy pattern for multiple payment providers
- ❌ Abstract provider interfaces
- ❌ Event sourcing
- ❌ Saga framework or custom workflow engine
- ❌ Generic payment state machine framework
- ❌ Admin replay endpoints (add as TODO only)
- ❌ Complex audit tables beyond what the plan specifies
- ❌ Unnecessary mappers/converters when direct construction is clearer
- ❌ Extra facade/coordinator/orchestrator layers unless they solve a real Spring transaction boundary problem
- ❌ Custom card input fields (use Stripe Hosted Checkout)

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
| `StripeSessionCreator` | `payment/api/StripeSessionCreator.java` | Returns `SessionWithClientSecretDto` wrapping `sessionId` + `clientSecret` — wrong for hosted checkout (should return URL) |
| `StripeWebhookService` | `payment/api/StripeWebhookService.java` | Handles `completed`, `expired`, `charge.refunded` — good base |
| `PaymentEndpoint` | `payment/endpoint/PaymentEndpoint.java` | `POST /payment` + `GET /payment/order` + webhook |
| `StripeSessionLineItemListConverter` | `payment/converter/...` | MapStruct cart→Stripe line items — reusable (but `longValue()` must be fixed to `longValueExact()`) |
| `OrderCreator` | `order/api/OrderCreator.java` | Two paths: `create()` (direct) and `createOrderAndDeleteCart(Session)` (Stripe) |
| `OrderStatusTransitioner` | `order/api/OrderStatusTransitioner.java` | State machine: `CREATED→PAID`, `PAID→SHIPPED→DELIVERED`, `CREATED/PAID→CANCELLED`, `PAID→REFUND_REQUESTED→REFUNDED` |
| `Order` entity | `order/entity/Order.java` | Has `stripePaymentIntentId` column — never populated |
| Stripe config | `application.yaml` | `stripe.enabled=${STRIPE_ENABLED:false}` (defaults to disabled), secret-key, webhook-secret |
| Dependency | `pom.xml` | `stripe-java:32.0.0` |
| Tests | 1 payment + 1 email | `StripeWebhookServiceTest` (signature validation only) + `PaymentEmailConfirmationTest` (in email package) |

### Backend — Critical Bugs

| # | Bug | Impact |
|---|-----|--------|
| 1 | `StripeSessionCreator` returns `SessionWithClientSecretDto` (wrapping `sessionId` + `clientSecret`) without setting `ui_mode` | Client secret is null for default hosted mode; should return `session.getUrl()` instead |
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
| Order success (legacy) | `features/orders/components/OrderSuccess.tsx` | Calls `GET /payment/order?sessionId=...` — live page but disconnected from current checkout submit flow |
| Cart store | `features/cart/state/cartStore.ts` | Zustand, dual-mode (guest/auth) |
| Orders API | `features/orders/api/ordersApi.ts` | Full CRUD with idempotency key support |
| API proxy | `app/api/proxy/[...path]/route.ts` | Proxies all calls with auth cookie forwarding |
| Stripe SDK | — | **Not installed** — zero `@stripe/*` packages |

### Frontend — Issues

| # | Issue | Impact |
|---|-------|--------|
| 1 | `useCheckoutForm` calls `POST /orders` directly | Orders created without payment |
| 2 | `resetCart()` called immediately after order creation | Cart cleared before payment confirmation |
| 3 | `OrderSuccess.tsx` disconnected from active checkout submit flow | Not reachable from current `/checkout` page |

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
      │ 15. Redirect to /checkout/success?session_id=...&order_id=...                        │
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

> **Deferred to later PR:** `PROCESSING` status (between `PAID` and `SHIPPED`) is valuable for order fulfillment workflow but is not part of Stripe payment integration. Adding it here would expand the PR scope unnecessarily. Implement in a separate "order fulfillment lifecycle" PR.

**New statuses (this PR):** `PENDING_PAYMENT`, `PAYMENT_FAILED`, `PAYMENT_EXPIRED`
**New events (this PR):** `PENDING_PAYMENT_CONFIRMED`, `PAYMENT_FAILED_EVENT`, `PAYMENT_EXPIRED_EVENT`

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

3. **StripeWebhookEvent for idempotency** — Dedicated table deduplicates webhook events by `stripe_event_id`. Uses **insert-first** pattern: attempt to insert the event ID before processing. If a duplicate key violation occurs, the event was already handled — return 200 immediately. This is safe under concurrent duplicate deliveries (unlike exists-then-process, which has a TOCTOU race).

4. **Webhook is the source of truth** — Only the webhook handler transitions `PENDING_PAYMENT → PAID`. The success page only reads status.

5. **Domain service separation** — Stripe service calls `OrderStatusTransitioner` and `PaymentRepository` — it does not directly mutate Order fields like status, stripePaymentIntentId, etc. The ChatGPT recommendation suggested `OrderCreator.markPaid()`, `markPaymentFailed()`, `markPaymentExpired()` methods, but the existing `OrderStatusTransitioner.transition()` already handles this cleanly via the state machine pattern. Adding `markPaid()` etc. to `OrderCreator` would bypass the state machine and duplicate transition logic. The correct approach: Stripe webhook service calls `orderStatusTransitioner.transition(orderId, event, actorId, reason)` — the same method used by admin endpoints and other callers.

6. **Hosted Checkout** — No Stripe frontend SDK needed. Stripe hosts the payment page. Minimal PCI scope.

---

## Backend Changes

### Phase 1: Payment Entity

A new entity to track payment provider details, separate from the Order.

**File:** `payment/entity/Payment.java`

```java
@Entity
@Table(name = "payments")
public class Payment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

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

    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;  // Amount in smallest currency unit (cents for USD)

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "raw_event_id")
    private String rawEventId;  // Stripe event ID that last updated this payment

    @Column(name = "latest_event_type")
    private String latestEventType;  // e.g. "checkout.session.completed"

    @Column(name = "checkout_idempotency_key", length = 100)
    private String checkoutIdempotencyKey;

    @Column(name = "checkout_request_hash", length = 64)
    private String checkoutRequestHash;  // SHA-256 of request payload — reject 409 if same key + different hash
}
```

> **Note:** `Payment` extends `AuditableEntity` (same as `Order`) to get automatic `createdAt`, `updatedAt`, `createdBy`, `updatedBy` via Spring Data JPA auditing. No manual timestamp management needed in service code.

**Enums:**

```java
public enum PaymentProvider { STRIPE }

public enum PaymentStatus {
    CREATED,                       // Payment record created, Stripe not yet called
    STRIPE_SESSION_CREATED,        // Stripe session created, awaiting user payment
    AWAITING_ASYNC_CONFIRMATION,   // checkout.session.completed fired but payment_status != paid (delayed payment method)
    PAID,                          // Stripe confirmed payment
    FAILED,                        // Stripe async payment failed
    EXPIRED,                       // Stripe session expired
    REFUNDED,                      // Stripe refund confirmed
    RECONCILIATION_FAILED          // Amount/currency mismatch — needs manual review
}
```

### Phase 2: StripeWebhookEvent Entity

Deduplicates webhook events. Uses insert-first pattern for concurrency safety.

**File:** `payment/entity/StripeWebhookEvent.java`

```java
@Entity
@Table(name = "stripe_webhook_events")
public class StripeWebhookEvent {

    @Id
    @Column(name = "stripe_event_id", nullable = false)
    private String stripeEventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WebhookEventStatus status;  // PROCESSING, PROCESSED, RETRYABLE_FAILED

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "failure_reason")
    private String failureReason;
}

public enum WebhookEventStatus { PROCESSING, PROCESSED, RETRYABLE_FAILED }
```

### Phase 3: Liquibase Migrations

**File:** `db/changelog/version-2.0/XX.XX.2026.part1.create-payments-table.sql`

```sql
CREATE TABLE IF NOT EXISTS public.payments (
    id                          UUID PRIMARY KEY,
    order_id                    UUID NOT NULL UNIQUE REFERENCES orders(id),
    user_id                     UUID NOT NULL,
    provider                    VARCHAR(20) NOT NULL DEFAULT 'STRIPE',
    provider_session_id         VARCHAR(255) UNIQUE,
    provider_payment_intent_id  VARCHAR(255),
    status                      VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    amount_minor                BIGINT NOT NULL,
    currency                    VARCHAR(3) NOT NULL DEFAULT 'usd',
    raw_event_id                VARCHAR(255),
    latest_event_type           VARCHAR(100),
    checkout_idempotency_key    VARCHAR(100),
    checkout_request_hash      VARCHAR(64),
    created_at                  TIMESTAMPTZ DEFAULT current_timestamp,
    updated_at                  TIMESTAMPTZ,
    created_by                  VARCHAR(255),
    updated_by                  VARCHAR(255)
);

CREATE INDEX idx_payments_order_id ON public.payments (order_id);
CREATE INDEX idx_payments_provider_session_id ON public.payments (provider_session_id);
CREATE INDEX idx_payments_provider_payment_intent_id
    ON public.payments (provider_payment_intent_id)
    WHERE provider_payment_intent_id IS NOT NULL;

-- Application-level checkout idempotency: same user + same key = same checkout
CREATE UNIQUE INDEX idx_payments_user_checkout_idempotency
    ON public.payments (user_id, checkout_idempotency_key)
    WHERE checkout_idempotency_key IS NOT NULL;
```

**File:** `db/changelog/version-2.0/XX.XX.2026.part2.create-stripe-webhook-events-table.sql`

```sql
CREATE TABLE IF NOT EXISTS public.stripe_webhook_events (
    stripe_event_id  VARCHAR(255) PRIMARY KEY,
    event_type       VARCHAR(100) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    received_at      TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    processed_at     TIMESTAMPTZ,
    failure_reason   TEXT
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
    - DELIVERY           # DEPRECATED — kept for backward compatibility
    - SHIPPED
    - FINISHED           # DEPRECATED — kept for backward compatibility
    - DELIVERED
    - CANCELLED
    - REFUND_REQUESTED
    - REFUNDED
```

> **Migration note:** `DELIVERY` and `FINISHED` are deprecated but must remain in the enum until all existing database rows are migrated. The data migration below should be done in a **separate PR** before or after the Stripe PR — do not mix historical status cleanup with payment integration.
>
> Separate migration (not in Stripe PR):
> ```sql
> UPDATE public.orders SET status = 'SHIPPED' WHERE status = 'DELIVERY';
> UPDATE public.orders SET status = 'DELIVERED' WHERE status = 'FINISHED';
> ```
> After the migration runs and is verified, the deprecated values can be removed in a future release.

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
          enum: [CREATED, STRIPE_SESSION_CREATED, AWAITING_ASYNC_CONFIRMATION, PAID, FAILED, EXPIRED, REFUNDED, RECONCILIATION_FAILED]
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
    // EXISTING: post-payment flows (unchanged for this PR)
    PAID, Map.of(SHIP, SHIPPED, CANCEL, CANCELLED, REQUEST_REFUND, REFUND_REQUESTED),
    SHIPPED, Map.of(DELIVER, DELIVERED),
    REFUND_REQUESTED, Map.of(REFUND_CONFIRMED, REFUNDED)
);
```

> **Deferred:** `PROCESSING` status and `START_PROCESSING` event will be added in a later "order fulfillment lifecycle" PR. For now, `PAID → SHIP → SHIPPED` remains the direct path.

### Phase 6: CheckoutPaymentService (Core Flow)

New service that orchestrates the checkout: validate → create order → create payment → call Stripe → return URL.

**File:** `payment/api/CheckoutPaymentService.java`

> **Spring self-invocation trap:** `@Transactional` on methods called from within the same class does not create real transaction boundaries (Spring proxy-based AOP). The transactional methods are extracted into a separate `CheckoutPaymentTransactionService` to ensure proper transaction demarcation.

```java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class CheckoutPaymentService {

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final CheckoutPaymentTransactionService txService;
    private final StripeCheckoutSessionCreator stripeSessionCreator;

    public CheckoutResponseDto checkout(CreateCheckoutRequestDto request, String idempotencyKey) {
        UserDto user = securityPrincipalProvider.get();
        UUID userId = user.getId();

        // --- Stage 1: DB transaction — validate, create order + payment, commit ---
        CheckoutPreparation prepared = txService.prepareCheckout(userId, request, idempotencyKey);

        // --- Idempotent retry: don't call Stripe with empty line items ---
        if (prepared.existing()) {
            return resolveExistingCheckout(prepared);
        }

        // --- Stage 2: Outside transaction — call Stripe ---
        StripeSessionResult stripeResult = stripeSessionCreator.create(
                prepared.order(), user.getEmail(), prepared.cartItems());

        // --- Stage 3: DB transaction — save Stripe details ---
        txService.saveStripeDetails(prepared.payment().getId(), stripeResult);

        log.info("checkout.created: orderId={}, stripeSessionId={}",
                prepared.order().getId(), mask(stripeResult.sessionId()));

        return new CheckoutResponseDto()
                .orderId(prepared.order().getId())
                .stripeSessionId(stripeResult.sessionId())
                .checkoutUrl(stripeResult.checkoutUrl());
    }

    private CheckoutResponseDto resolveExistingCheckout(CheckoutPreparation prepared) {
        Payment payment = prepared.payment();

        // IMPORTANT: Session.retrieve() is a remote Stripe API call and MUST remain
        // outside any @Transactional method. This method lives in CheckoutPaymentService
        // (the non-transactional coordinator), not in CheckoutPaymentTransactionService.

        // Case A: Stripe session already created — return stored URL if still valid
        if (payment.getProviderSessionId() != null) {
            try {
                Session session = Session.retrieve(payment.getProviderSessionId());
                if ("expired".equals(session.getStatus())) {
                    // Session expired — tell frontend to start a new checkout
                    throw new BadRequestException(
                            "Previous checkout session expired. Please retry with a new Idempotency-Key.");
                }
                return new CheckoutResponseDto()
                        .orderId(prepared.order().getId())
                        .stripeSessionId(payment.getProviderSessionId())
                        .checkoutUrl(session.getUrl());
            } catch (StripeException e) {
                throw new StripeSessionCreationException("Failed to retrieve existing session", e);
            }
        }

        // Case B: Order+Payment created but Stripe call failed last time — retry.
        // Rebuild line items from the persisted Order.items snapshot (NOT empty list).
        // Stripe idempotency compares request parameters — they must match the original.
        Order order = prepared.order();
        List<SessionCreateParams.LineItem> lineItems = order.getItems().stream()
                .map(item -> SessionCreateParams.LineItem.builder()
                        .setQuantity((long) item.getProductsQuantity())
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(item.getProductPrice()
                                        .multiply(BigDecimal.valueOf(100))
                                        .setScale(0, RoundingMode.UNNECESSARY)
                                        .longValueExact())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(item.getProductName())
                                        .build())
                                .build())
                        .build())
                .toList();

        StripeSessionResult stripeResult = stripeSessionCreator.createFromLineItems(
                order, securityPrincipalProvider.get().getEmail(), lineItems);

        txService.saveStripeDetails(payment.getId(), stripeResult);

        return new CheckoutResponseDto()
                .orderId(order.getId())
                .stripeSessionId(stripeResult.sessionId())
                .checkoutUrl(stripeResult.checkoutUrl());
    }
}
```

**File:** `payment/api/CheckoutPaymentTransactionService.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutPaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderCreator orderCreator;
    private final ShoppingCartService shoppingCartService;

    @Transactional
    CheckoutPreparation prepareCheckout(UUID userId,
                                        CreateCheckoutRequestDto request,
                                        String idempotencyKey) {
        // Application-level idempotency: same user + same key → return existing
        Optional<Payment> existing = paymentRepository
                .findByCheckoutIdempotencyKeyAndUserId(idempotencyKey, userId);
        if (existing.isPresent()) {
            // Idempotent hit: same user + same key → return existing order/payment.
            // Do NOT read the live cart here — it may already be deleted after
            // successful payment (webhook deletes cart). The checkout_request_hash
            // was computed from the full snapshot (request + cart) at creation time
            // and cannot be recomputed without the original cart state.
            //
            // For MVP: trust the idempotency key match. The hash was validated at
            // creation time. If exact re-validation is needed later, store the
            // original snapshot JSON on Payment.
            Order order = orderRepository.findById(existing.get().getOrderId()).orElseThrow();
            log.info("checkout.idempotent_hit: userId={}, key={}", userId, idempotencyKey);
            return new CheckoutPreparation(order, existing.get(), List.of(), true);
        }

        ShoppingCartDto cart = shoppingCartService.getByUserIdOrThrow(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot checkout: shopping cart is empty");
        }

        Order order = orderCreator.createPendingPaymentOrder(userId, request, cart);

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .userId(userId)
                .provider(PaymentProvider.STRIPE)
                .status(PaymentStatus.CREATED)
                .amountMinor(toMinorUnits(order.getItemsTotalPrice()))
                .currency("usd")
                .checkoutIdempotencyKey(idempotencyKey)
                .checkoutRequestHash(computeCheckoutSnapshotHash(request, cart))
                .build();
        payment = paymentRepository.save(payment);

        return new CheckoutPreparation(order, payment, cart.getItems(), false);
    }

    @Transactional
    public void saveStripeDetails(UUID paymentId, StripeSessionResult stripeResult) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        payment.setProviderSessionId(stripeResult.sessionId());
        payment.setStatus(PaymentStatus.STRIPE_SESSION_CREATED);
        paymentRepository.save(payment);
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    /**
     * SHA-256 hash of the full checkout snapshot: recipient fields, address,
     * cart item IDs/quantities/prices, currency, and total amount.
     * Used to detect Idempotency-Key reuse with different checkout parameters.
     */
    private String computeCheckoutSnapshotHash(CreateCheckoutRequestDto request,
                                                ShoppingCartDto cart) {
        StringBuilder sb = new StringBuilder();
        // Recipient / address fields
        sb.append(request.getRecipientName()).append('|');
        sb.append(request.getRecipientSurname()).append('|');
        sb.append(request.getRecipientPhone()).append('|');
        sb.append(request.getDeliveryAddressId()).append('|');
        // Cart items — sorted by productId for deterministic ordering
        cart.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProductInfo().getId()))
                .forEach(item -> {
                    sb.append(item.getProductInfo().getId()).append(':');
                    sb.append(item.getProductQuantity()).append(':');
                    sb.append(item.getProductInfo().getPrice()).append('|');
                });
        // Currency and total
        sb.append("usd").append('|');
        sb.append(toMinorUnits(cart.getItemsTotalPrice()));
        return DigestUtils.sha256Hex(sb.toString());
    }
}

record CheckoutPreparation(
        Order order,
        Payment payment,
        List<ShoppingCartItemDto> cartItems,
        boolean existing) {}
```

> **Why two services?** Spring's `@Transactional` uses proxy-based AOP. Calling a `@Transactional` method from within the same class bypasses the proxy — the transaction annotation is silently ignored. By extracting transactional methods into `CheckoutPaymentTransactionService`, each `@Transactional` call goes through the Spring proxy and creates a real transaction boundary.

### Phase 7: Update OrderCreator

Add a new method for creating pending-payment orders. Keep the existing `create()` for non-Stripe flows.

**File:** `order/api/OrderCreator.java` — add method:

```java
@Transactional
public Order createPendingPaymentOrder(UUID userId, CreateCheckoutRequestDto request,
                                       ShoppingCartDto cart) {
    // Address resolution: CreateCheckoutRequestDto has the same field names
    // (deliveryAddressId, address) as CreateNewOrderRequestDto, but they are
    // different generated types. Extract the common logic into a helper.
    Address deliveryAddress = resolveDeliveryAddress(
            request.getDeliveryAddressId(), request.getAddress(), userId);

    // Snapshot cart items into order items (same as existing create())
    List<OrderItem> items = cart.getItems().stream()
            .map(orderDtoConverter::toOrderItem)
            .toList();

    Order order = Order.builder()
            .userId(userId)
            .sessionId(UUID.randomUUID().toString())  // placeholder — see note below
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

**Important:** The existing `resolveAddress(CreateNewOrderRequestDto, UUID)` must be refactored. Extract the common logic into a new shared method:

```java
// New shared method — called by both create() and createPendingPaymentOrder()
private Address resolveDeliveryAddress(UUID deliveryAddressId, AddressDto inlineAddress, UUID userId) {
    if (deliveryAddressId != null) {
        DeliveryAddressEntity saved = deliveryAddressRepository
                .findByIdAndUserId(deliveryAddressId, userId)
                .orElseThrow(() -> new BadRequestException(
                        "Delivery address not found: " + deliveryAddressId));
        return snapshotAddress(saved);
    }
    if (inlineAddress == null) {
        throw new BadRequestException("Either 'deliveryAddressId' or 'address' must be provided.");
    }
    return Address.builder()
            .country(inlineAddress.getCountry())
            .city(inlineAddress.getCity())
            .line(inlineAddress.getLine())
            .postcode(inlineAddress.getPostcode())
            .build();
}

// Existing method updated to delegate:
private Address resolveAddress(CreateNewOrderRequestDto request, UUID userId) {
    return resolveDeliveryAddress(request.getDeliveryAddressId(), request.getAddress(), userId);
}
```

**Remove:** `createOrderAndDeleteCart(Session stripeSession)` — this method reads the live cart at webhook time and is the source of the cart mutation bug. It is fully replaced by the order-first flow.

> **Note on `Order.sessionId` placeholder:** The `sessionId` column is `NOT NULL` and `updatable=false`, so `createPendingPaymentOrder()` sets it to a random UUID placeholder. This is a code smell — the field originally held the Stripe session ID, but now the Stripe session ID lives on `Payment.providerSessionId`. Recommended cleanup (can be in this PR or a follow-up):
> - **Option A:** Make `orders.session_id` nullable via migration (`ALTER TABLE orders ALTER COLUMN session_id DROP NOT NULL`), then stop setting it for Stripe orders.
> - **Option B:** Repurpose it as an internal order reference (e.g., `"IL-" + shortId`).
> - **Option C:** Leave the placeholder for now and clean up in a later PR. Acceptable for MVP.
>
> **TODO / Follow-up issue:** This placeholder is acceptable for the initial Stripe PR to avoid scope creep. Create a follow-up ticket: _"Make `orders.session_id` nullable or replace with internal order reference"_. The placeholder does not affect correctness — it is only a readability/design smell.

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

    /**
     * Cart-based entry point (normal checkout). Converts cart items to Stripe
     * line items, then delegates to createFromLineItems().
     */
    public StripeSessionResult create(Order order, String customerEmail,
                                      List<ShoppingCartItemDto> cartItems) {
        List<SessionCreateParams.LineItem> lineItems = lineItemConverter.toLineItems(cartItems);
        return createFromLineItems(order, customerEmail, lineItems);
    }

    /**
     * Core method that both normal checkout and idempotent retry use.
     * Accepts pre-built Stripe line items so the retry path can rebuild them
     * from the persisted Order.items snapshot without needing the original cart.
     */
    public StripeSessionResult createFromLineItems(Order order, String customerEmail,
                                                   List<SessionCreateParams.LineItem> lineItems) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(customerEmail)
                .setSuccessUrl(frontendUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}&order_id=" + order.getId())
                .setCancelUrl(frontendUrl + "/checkout/cancel?order_id=" + order.getId())
                .setClientReferenceId(order.getId().toString())
                .putMetadata("orderId", order.getId().toString())
                .putMetadata("userId", order.getUserId().toString())
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("orderId", order.getId().toString())
                                .putMetadata("userId", order.getUserId().toString())
                                .build())
                .addAllLineItem(lineItems)
                .addAllShippingOption(shippingOptions())
                .setExpiresAt(OffsetDateTime.now().plusMinutes(31).toEpochSecond())
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
- Sets `expires_at` (31 min — Stripe requires minimum 30 min; 31 avoids clock-skew edge case)
- `create()` delegates to `createFromLineItems()` — the retry path in `resolveExistingCheckout()` calls `createFromLineItems()` directly with line items rebuilt from `Order.items`

### Phase 9: Rewrite StripeWebhookService

The webhook handler now transitions existing orders instead of creating them.

**File:** `payment/api/StripeWebhookService.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripeWebhookService {

    private final StripeWebhookEventRecorder webhookEventRecorder;
    private final StripeWebhookBusinessProcessor webhookBusinessProcessor;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    /**
     * Not @Transactional itself — event acquisition and business processing
     * use separate transaction boundaries via separate beans.
     */
    public void processWebhook(String payload, String stripeSignature) {
        Event event = parseEvent(payload, stripeSignature);

        if (!webhookEventRecorder.tryAcquire(event.getId(), event.getType())) {
            log.info("payment.webhook.duplicate: eventId={}", event.getId());
            return;
        }

        try {
            webhookBusinessProcessor.process(event);
            webhookEventRecorder.markProcessed(event.getId());
        } catch (Exception e) {
            // Business processor returns normally for non-retryable failures
            // (e.g., amount mismatch → RECONCILIATION_FAILED is persisted inside TX).
            // Only transient/unexpected failures reach this catch block.
            // Mark RETRYABLE_FAILED so Stripe retries can re-acquire the event.
            try {
                webhookEventRecorder.markRetryableFailed(event.getId(), e.getMessage());
            } catch (Exception markerFailure) {
                log.error("payment.webhook.failed_to_mark_failed: eventId={}",
                        event.getId(), markerFailure);
            }
            throw e;  // Stripe will retry transient failures
        }

        log.info("payment.webhook.processed: eventType={}, eventId={}", event.getType(), event.getId());
    }

    // parseEvent() — unchanged from current implementation
}
```

**File:** `payment/api/StripeWebhookBusinessProcessor.java`

> **Why a separate bean?** Spring's proxy-based `@Transactional` does not apply on self-invocation (calling a `@Transactional` method from within the same class). Extracting business processing into its own bean ensures the `@Transactional` annotation is honored.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookBusinessProcessor {

    private final OrderStatusTransitioner orderStatusTransitioner;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ShoppingCartRepository shoppingCartRepository;

    @Transactional
    public void process(Event event) {
        switch (event.getType()) {
            case "checkout.session.completed" -> handleSessionCompleted(event, requireSession(event));
            case "checkout.session.expired" -> handleExpired(requireSession(event));
            case "checkout.session.async_payment_succeeded" -> handleSessionCompleted(event, requireSession(event));
            case "checkout.session.async_payment_failed" -> handleAsyncPaymentFailed(requireSession(event));
            case "charge.refunded" -> handleChargeRefunded(event);
            default -> log.debug("payment.webhook.unhandled: eventType={}", event.getType());
        }
    }

    private void handleSessionCompleted(Event event, Session stripeSession) {
        // For delayed payment methods, checkout.session.completed fires but
        // payment_status may not be "paid" yet. Only mark PAID when actually paid.
        if (!"paid".equals(stripeSession.getPaymentStatus())) {
            UUID orderId = extractOrderId(stripeSession);
            paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.AWAITING_ASYNC_CONFIRMATION);
                payment.setRawEventId(event.getId());
                payment.setLatestEventType(event.getType());
                paymentRepository.save(payment);
            });
            log.info("payment.awaiting_async: orderId={}, paymentStatus={}",
                    orderId, stripeSession.getPaymentStatus());
            return;  // Order stays PENDING_PAYMENT. Wait for async_payment_succeeded.
        }
        markPaid(event, stripeSession);
    }

    private void markPaid(Event event, Session stripeSession) {
        UUID orderId = extractOrderId(stripeSession);

        // Lock payment row to prevent concurrent processing of different events
        // targeting the same order (e.g., completed + async_payment_succeeded).
        // Event-id dedupe alone is not enough — different events can have different IDs.
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalStateException("No Payment for orderId=" + orderId));

        // Already paid — idempotent
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("payment.already_paid: orderId={}", orderId);
            return;
        }

        // Verify amount/currency match (reconciliation guard).
        // On mismatch: mark RECONCILIATION_FAILED and return normally.
        // Do NOT throw — this method is @Transactional, and an unchecked exception
        // would roll back the RECONCILIATION_FAILED status we just persisted.
        // The business processor returns normally; the coordinator marks the webhook
        // event as PROCESSED. The Payment entity itself tracks the real outcome
        // (RECONCILIATION_FAILED). The webhook event status only tracks delivery handling.
        Long stripeAmount = stripeSession.getAmountTotal();
        String stripeCurrency = stripeSession.getCurrency();
        if (stripeAmount != null && stripeCurrency != null) {
            if (!stripeAmount.equals(payment.getAmountMinor())
                    || !stripeCurrency.equalsIgnoreCase(payment.getCurrency())) {
                log.error("payment.amount_mismatch: orderId={}, expected={}_{}, stripe={}_{}",
                        orderId, payment.getAmountMinor(), payment.getCurrency(),
                        stripeAmount, stripeCurrency);
                payment.setStatus(PaymentStatus.RECONCILIATION_FAILED);
                payment.setRawEventId(event.getId());
                payment.setLatestEventType(event.getType());
                paymentRepository.save(payment);
                return;  // Transaction commits — RECONCILIATION_FAILED is persisted
            }
        }

        // Update Payment
        payment.setProviderPaymentIntentId(stripeSession.getPaymentIntent());
        payment.setStatus(PaymentStatus.PAID);
        payment.setRawEventId(event.getId());
        payment.setLatestEventType(event.getType());
        paymentRepository.save(payment);

        // Update Order: PENDING_PAYMENT → PAID
        Order order = orderStatusTransitioner.transition(
                orderId, OrderEvent.PENDING_PAYMENT_CONFIRMED, null, "Stripe payment confirmed");

        // Store stripePaymentIntentId on Order (for refund lookup).
        // DESIGN EXCEPTION: This directly mutates an Order field outside OrderStatusTransitioner.
        // Accepted because stripePaymentIntentId is integration metadata, not business state.
        // The existing refund flow (handleChargeRefunded) searches Order by this field.
        // Long-term, refund lookup could use Payment.providerPaymentIntentId instead,
        // removing this field from Order entirely.
        order.setStripePaymentIntentId(stripeSession.getPaymentIntent());
        orderRepository.save(order);

        // Delete cart
        shoppingCartRepository.deleteByUserId(order.getUserId());

        // Send confirmation email AFTER transaction commits.
        // The existing OrderStatusChangedEvent (published by orderStatusTransitioner.transition())
        // can trigger email via @TransactionalEventListener(phase = AFTER_COMMIT).
        // Do NOT call paymentEmailConfirmation.send() inside this @Transactional method —
        // if the transaction rolls back after the email is sent, the user gets a
        // confirmation email but the order is not actually PAID.

        log.info("checkout.completed: orderId={}, paymentIntentId={}",
                orderId, stripeSession.getPaymentIntent());
    }

    private void handleExpired(Session stripeSession) {
        UUID orderId = extractOrderId(stripeSession);

        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.EXPIRED);
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

    // requireSession() — extracts Session from event data object.
    // Moved here from StripeWebhookService because it is only used by the business processor.
    // extractOrderId() — reads metadata.orderId from Session.
}
```

**Key changes from current `StripeWebhookService`:**
- `StripeWebhookService` is now a thin coordinator — no `@Transactional` on itself
- Business logic extracted into `StripeWebhookBusinessProcessor` (separate bean, avoids self-invocation trap)
- Event acquisition/status via `StripeWebhookEventRecorder` with `REQUIRES_NEW` transactions
- `handleSessionCompleted()` checks `payment_status` before marking PAID; persists `AWAITING_ASYNC_CONFIRMATION` for delayed payment methods
- `markPaid()` uses `findByOrderIdForUpdate()` (PESSIMISTIC_WRITE) to prevent concurrent processing of different events targeting the same order
- Amount/currency mismatch marks `RECONCILIATION_FAILED` and returns 200 (no endless Stripe retries)
- Sets `stripePaymentIntentId` on Order (documented design exception for refund lookup)
- `PaymentEmailConfirmation` removed — email sent via `@TransactionalEventListener(phase = AFTER_COMMIT)` on `OrderStatusChangedEvent`

**File:** `payment/api/StripeWebhookEventRecorder.java`

```java
@Service
@RequiredArgsConstructor
public class StripeWebhookEventRecorder {

    private final StripeWebhookEventRepository repository;

    /**
     * REQUIRES_NEW: committed independently so the PROCESSING marker
     * persists even if business processing rolls back.
     * If the event already exists with RETRYABLE_FAILED status (transient failure
     * on previous attempt), re-acquire it by resetting to PROCESSING.
     * If PROCESSING or PROCESSED, return false.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquire(String eventId, String eventType) {
        try {
            repository.saveAndFlush(new StripeWebhookEvent(
                    eventId, eventType, WebhookEventStatus.PROCESSING,
                    OffsetDateTime.now(), null, null));
            return true;
        } catch (DataIntegrityViolationException e) {
            // Row exists — only re-acquire if RETRYABLE_FAILED
            return repository.findById(eventId)
                    .filter(evt -> evt.getStatus() == WebhookEventStatus.RETRYABLE_FAILED)
                    .map(evt -> {
                        evt.setStatus(WebhookEventStatus.PROCESSING);
                        evt.setFailureReason(null);
                        return true;
                    })
                    .orElse(false);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(String eventId) {
        repository.findById(eventId).ifPresent(e -> {
            e.setStatus(WebhookEventStatus.PROCESSED);
            e.setProcessedAt(OffsetDateTime.now());
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetryableFailed(String eventId, String reason) {
        repository.findById(eventId).ifPresent(e -> {
            e.setStatus(WebhookEventStatus.RETRYABLE_FAILED);
            e.setFailureReason(reason);
        });
    }
}
```

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
            @Valid @RequestBody CreateCheckoutRequestDto request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        CheckoutResponseDto response = checkoutPaymentService.checkout(request, idempotencyKey);
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
    private final SecurityPrincipalProvider securityPrincipalProvider;

    public CheckoutStatusDto getStatus(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Enforce ownership: user can only read their own order status
        UserDto currentUser = securityPrincipalProvider.get();
        if (!order.getUserId().equals(currentUser.getId())) {
            throw new OrderAccessDeniedException(orderId);
        }

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

export type PaymentStatusValue = 'CREATED' | 'STRIPE_SESSION_CREATED' | 'AWAITING_ASYNC_CONFIRMATION' | 'PAID' | 'FAILED' | 'EXPIRED' | 'REFUNDED' | 'RECONCILIATION_FAILED'

export interface CheckoutStatus {
  orderId: string
  orderStatus: string
  paymentStatus?: PaymentStatusValue
}

export async function createCheckout(
  payload: CreateCheckoutRequest,
  idempotencyKey: string,
): Promise<CheckoutResponse> {
  const { data } = await api.post<CheckoutResponse>('/payment/checkout', payload, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
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
export type {
  CreateCheckoutRequest,
  CheckoutResponse,
  CheckoutStatus,
  PaymentStatusValue,
} from './api/paymentApi'
```

### Phase 2: Update useCheckoutForm Hook

**File:** `src/features/checkout/hooks/useCheckoutForm.ts`

Replace the `createOrder()` call with `createCheckout()` + redirect:

```ts
// BEFORE (current):
await createOrder(payload, idempotencyKey)  // return value discarded
resetCart()
router.push('/orders')

// AFTER:
const idempotencyKey = crypto.randomUUID()
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
}, idempotencyKey)

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
  const { order_id } = await searchParams
  if (!order_id) redirect('/orders')
  return <CheckoutSuccess orderId={order_id} />
}
```

> **Note:** The Stripe `success_url` is set to include both `session_id={CHECKOUT_SESSION_ID}` (Stripe template variable) and `order_id=<orderId>` (hardcoded at session creation time). The FE uses `order_id` to poll the backend status endpoint. The `session_id` is available as a fallback but not needed for the primary flow.

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

export default async function CheckoutCancelPage({
  searchParams,
}: {
  searchParams: Promise<{ order_id?: string }>
}) {
  await requireRecoverableSession('/checkout')
  const { order_id } = await searchParams
  return (
    <div>
      <h1>Payment cancelled</h1>
      <p>Your cart is still intact. <Link href="/checkout">Return to checkout</Link> or <Link href="/cart">edit your cart</Link>.</p>
    </div>
  )
}
```

> **PENDING_PAYMENT cleanup on cancel:** When the user cancels on Stripe's page, the `PENDING_PAYMENT` order is NOT immediately cancelled. The Stripe session will expire after 30 minutes, triggering a `checkout.session.expired` webhook that transitions the order to `PAYMENT_EXPIRED`. This is acceptable because:
> - The user can retry checkout (creates a new order)
> - The expired order is cleaned up automatically by the webhook
> - No manual cancellation API call is needed from the cancel page
>
> If immediate cancellation is desired in a future iteration, the cancel page could call `POST /orders/{order_id}/cancel` using the `order_id` from the URL.

### Phase 5: Remove Legacy Payment Code

**Delete:**
- `src/app/orders/success/page.tsx` — old Stripe redirect handler
- `src/features/orders/components/OrderSuccess.tsx` — old verification component

These are replaced by `/checkout/success` and `CheckoutSuccess`.

### Phase 6: Update Order History Filters

The `OrderHistory` component currently has status filter tabs: All, Placed (CREATED), Paid, Shipped, Delivered, Cancelled. With the new statuses, add:

- **Pending** tab → filters by `PENDING_PAYMENT` (orders awaiting payment)
- Optionally group `PAYMENT_FAILED` and `PAYMENT_EXPIRED` under the existing **Cancelled** tab, or add a **Failed** tab

**File:** `src/features/orders/components/OrderHistory.tsx` — update the status filter tabs array.

**File:** `src/features/orders/components/OrderStatusBadge.tsx` — add badge styles for `PENDING_PAYMENT`, `PAYMENT_FAILED`, `PAYMENT_EXPIRED`.

### Phase 7: Cart Behavior Changes

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
| | Idempotent retry after cart deleted returns existing order/payment without reading cart |
| | Idempotent retry when `providerSessionId` is null rebuilds line items from `Order.items` |
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
| | Amount/currency mismatch marks `RECONCILIATION_FAILED` and returns 200 (non-retryable) |
| | Amount/currency mismatch persists `RECONCILIATION_FAILED` on Payment (no rollback) |
| | Transient DB failure rethrows so Stripe can retry |
| | `RETRYABLE_FAILED` event is re-acquired on Stripe retry |
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
| `payment/repository/PaymentRepository.java` | JPA repository: `findByOrderId()`, `findByOrderIdForUpdate()` (PESSIMISTIC_WRITE), `findByProviderSessionId()`, `findByCheckoutIdempotencyKeyAndUserId()` |
| `payment/repository/StripeWebhookEventRepository.java` | JPA repository: `existsById()` |
| `payment/api/CheckoutPaymentService.java` | Orchestrates: validate → create order → create payment → call Stripe |
| `payment/api/CheckoutPaymentTransactionService.java` | Transactional methods for checkout (extracted to avoid Spring self-invocation trap) |
| `payment/api/StripeCheckoutSessionCreator.java` | Creates Stripe Hosted Checkout Session with idempotency |
| `payment/api/StripeSessionResult.java` | Record: `sessionId`, `checkoutUrl` |
| `payment/api/CheckoutPreparation.java` | Record: `order`, `payment`, `cartItems`, `existing` |
| `payment/api/PaymentStatusService.java` | Read-only status for success page polling |
| `payment/api/StripeWebhookEventRecorder.java` | REQUIRES_NEW transactional methods for webhook event acquisition/status |
| `payment/api/StripeWebhookBusinessProcessor.java` | @Transactional business logic for webhook events (extracted to avoid self-invocation) |
| `db/.../create-payments-table.sql` | Liquibase migration |
| `db/.../create-stripe-webhook-events-table.sql` | Liquibase migration |

### Backend — Modified Files

| File | Changes |
|------|---------|
| `order/api/OrderCreator.java` | Add `createPendingPaymentOrder()`. Remove `createOrderAndDeleteCart()`. |
| `order/api/OrderStatusTransitioner.java` | Add `PENDING_PAYMENT` transitions in `TRANSITIONS` map |
| `payment/endpoint/PaymentEndpoint.java` | New `POST /checkout`, `GET /checkout/{orderId}/status`. Remove old endpoints. |
| `payment/api/StripeWebhookService.java` | Transition orders instead of creating them. Add event deduplication. Handle async events. Remove `processRedirect()`. |
| `payment/converter/StripeSessionLineItemListConverter.java` | Fix `toStripeUnitAmount()`: change `longValue()` to `.setScale(0, RoundingMode.UNNECESSARY).longValueExact()` to prevent silent truncation |
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
| `features/orders/components/OrderHistory.tsx` | Add filter tabs for `PENDING_PAYMENT` and optionally `PAYMENT_FAILED`/`PAYMENT_EXPIRED` |
| `features/orders/components/OrderStatusBadge.tsx` | Add badge styles for new order statuses |

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

> ⚠️ **Test mode only.** All Stripe keys below are test/sandbox keys. No real money is charged. Use [Stripe test cards](https://docs.stripe.com/testing#cards) only — never enter real card details.

```bash
# 1. Set env vars (add to .env — test mode only)
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
- [Stripe Test Mode & Test Cards](https://docs.stripe.com/testing) — test API keys, test card numbers, sandbox payments
- [Stripe client_reference_id](https://docs.stripe.com/api/checkout/sessions) — reconcile sessions with internal systems

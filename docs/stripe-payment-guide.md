# Stripe Payment Integration in Iced Latte

A beginner-friendly guide to how Iced Latte integrates with Stripe for payment processing. Written in 2026.

> **Test mode only.** Iced Latte uses Stripe sandbox. No real money is charged. No real cards are used. No live keys are used.

---

## Table of Contents

1. [What This Guide Covers](#what-this-guide-covers)
2. [The Big Picture](#the-big-picture)
3. [The Payment Flow Step by Step](#the-payment-flow-step-by-step)
4. [Backend Architecture](#backend-architecture)
5. [Frontend Architecture](#frontend-architecture)
6. [Database Design](#database-design)
7. [Webhook Processing](#webhook-processing)
8. [Safety Mechanisms](#safety-mechanisms)
9. [Configuration](#configuration)
10. [Local Development Setup](#local-development-setup)
11. [Testing with Stripe Test Cards](#testing-with-stripe-test-cards)
12. [Glossary](#glossary)

---

## What This Guide Covers

This guide explains how a typical backend integrates with a payment provider like Stripe. Iced Latte is an open-source online supermarket built for learning. The Stripe integration exists so you can understand:

- How a backend creates orders and tracks payment state in its own database
- How Stripe Hosted Checkout works (redirect-based, no custom card form)
- How webhooks deliver payment confirmation from Stripe to your server
- How to protect against duplicate events, race conditions, and state corruption
- How frontend and backend coordinate during the payment lifecycle

You do **not** need prior Stripe experience to follow this guide.

---

## The Big Picture

Iced Latte owns the business logic. Stripe is just the payment rail.

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────┐
│   Browser    │     │  Iced Latte     │     │   Stripe    │
│  (Next.js)   │     │  Backend        │     │   (test)    │
│              │     │  (Spring Boot)  │     │             │
└──────┬───────┘     └────────┬────────┘     └──────┬──────┘
       │                      │                      │
       │  1. Place order      │                      │
       │─────────────────────>│                      │
       │                      │                      │
       │                      │  2. Create local     │
       │                      │     Order + Payment  │
       │                      │                      │
       │                      │  3. Create Checkout  │
       │                      │     Session          │
       │                      │─────────────────────>│
       │                      │                      │
       │                      │  4. Return session   │
       │                      │     URL              │
       │                      │<─────────────────────│
       │                      │                      │
       │  5. Redirect to      │                      │
       │     Stripe Checkout  │                      │
       │<─────────────────────│                      │
       │                      │                      │
       │  6. User pays on     │                      │
       │     Stripe page      │                      │
       │─────────────────────────────────────────────>│
       │                      │                      │
       │  7. Redirect back    │  8. Webhook:         │
       │     to success page  │     payment confirmed│
       │<─────────────────────│<─────────────────────│
       │                      │                      │
       │  9. Poll status      │  9. Update Payment   │
       │─────────────────────>│     + Order to PAID  │
       │                      │     + clear cart     │
       │  10. Show ✅          │                      │
       │<─────────────────────│                      │
```

**Key principle:** The backend creates the order and payment record *before* calling Stripe. Stripe only collects the card details and processes the charge. The backend learns about the result through webhooks (primary) or by polling Stripe directly (fallback).

---

## The Payment Flow Step by Step

### Step 1 — User clicks "Place order"

The frontend checkout form collects recipient name, phone, and delivery address. When the user clicks "Place order":

- The frontend generates a unique `Idempotency-Key` (UUID) to prevent duplicate orders
- It calls `POST /api/v1/payment/checkout` with the recipient info and the idempotency key

```typescript
// Frontend: useCheckoutForm.ts
const idempotencyKey = crypto.randomUUID()
const checkout = await createCheckout(
  { recipientName, recipientSurname, recipientPhone, address },
  idempotencyKey,
)
```

The frontend does **not** clear the cart at this point. The cart is only cleared after payment is confirmed.

### Step 2 — Backend validates and creates local Order + Payment

The backend receives the request and runs **Transaction A**:

1. Checks if this idempotency key was already used (returns existing order if so)
2. Loads the user's shopping cart
3. Validates the cart is not empty
4. Creates an `Order` with status `PENDING_PAYMENT` — snapshots cart items into order items
5. Creates a `Payment` record with status `CREATED`
6. Commits the transaction

```java
// CheckoutPaymentTransactionService.prepareCheckout()
Order order = orderCreator.createPendingPaymentOrder(userId, request, cart);
Payment payment = Payment.builder()
    .orderId(order.getId())
    .status(PaymentStatus.CREATED)
    .amountMinor(toMinorUnits(order.getItemsTotalPrice()))
    .currency("usd")
    .checkoutIdempotencyKey(idempotencyKey)
    .build();
```

**Why create the order before calling Stripe?** Because if the Stripe call fails, you still have a local record of what the user tried to buy. You can retry without losing data.

### Step 3 — Backend calls Stripe API (outside the transaction)

After Transaction A commits, the backend calls Stripe to create a Checkout Session. This happens **outside** any database transaction because:

- Network calls to external services should never hold a DB transaction open
- If Stripe is slow or down, you don't want to lock database rows

```java
// CheckoutPaymentService.checkout()
// Stage 1: DB transaction — validate, create order + payment
CheckoutPreparation prepared = txService.prepareCheckout(userId, request, idempotencyKey);

// Stage 2: Outside transaction — call Stripe
StripeSessionResult stripeResult = stripeSessionCreator.create(
    prepared.order(), user.getEmail(), prepared.cartItems());

// Stage 3: DB transaction — save Stripe details
txService.saveStripeDetails(prepared.payment().getId(), stripeResult);
```

The Stripe session includes:
- Line items with product names, quantities, and prices (built dynamically from the cart — no Stripe Product Catalog needed)
- Success and cancel redirect URLs
- Shipping options
- Order ID in metadata (so webhooks can find the order)
- A 31-minute expiry

### Step 4 — Backend returns the checkout URL

The backend saves the Stripe session ID in **Transaction B**, then returns the checkout URL to the frontend:

```json
{
  "orderId": "13f68620-e855-4a3a-bc29-e699ded0cc3e",
  "stripeSessionId": "cs_test_b1Cx...",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_b1Cx..."
}
```

### Step 5 — Frontend redirects to Stripe

The frontend redirects the browser to Stripe's hosted checkout page:

```typescript
// useCheckoutForm.ts
window.location.href = checkout.checkoutUrl
```

The user now sees Stripe's payment form with their cart items, shipping options, and a card input. Iced Latte never touches card details — Stripe handles all of that.

### Step 6 — User pays on Stripe

The user enters a test card number (e.g., `4242 4242 4242 4242`) and clicks Pay. Stripe processes the payment in sandbox mode.

### Step 7 — Stripe redirects back to success page

After payment, Stripe redirects the browser to:
```
http://localhost:3000/checkout/success?order_id=13f68620-...
```

### Step 8 — Stripe sends a webhook to the backend

Simultaneously, Stripe sends a `checkout.session.completed` webhook event to:
```
POST /api/v1/payment/stripe/webhook
```

The backend verifies the webhook signature, finds the order, checks the amount matches, and updates:
- Payment status → `PAID`
- Order status → `PAID`
- Deletes the shopping cart

### Step 9 — Success page polls backend status

The success page polls `GET /api/v1/payment/checkout/{orderId}/status` every 2 seconds (up to 5 times).

**Primary path:** The webhook already updated the status, so the first poll returns `PAID`.

**Fallback path:** If the webhook hasn't arrived yet (common in local dev without Stripe CLI), the backend calls `Session.retrieve()` to check Stripe directly and updates the status. Real payment systems always have this reconciliation fallback — never rely on a single delivery mechanism for money.

### Step 10 — Frontend shows confirmation

When the poll returns `orderStatus: "PAID"`, the success page:
- Shows ✅ "Payment confirmed!"
- Resets the frontend cart state
- Links to the orders page

```typescript
// CheckoutSuccess.tsx
if (result.orderStatus === 'PAID') {
  setStatus('paid')
  resetCart()  // Clear frontend cart only after confirmed payment
}
```

---

## Backend Architecture

### Package Structure

All payment code lives in `src/main/java/com/zufar/icedlatte/payment/`:

```
payment/
├── endpoint/
│   └── PaymentEndpoint.java          — REST controller (3 endpoints)
├── api/
│   ├── CheckoutPaymentService.java          — Checkout orchestrator (non-transactional)
│   ├── CheckoutPaymentTransactionService.java — DB transactions for checkout
│   ├── CheckoutPreparation.java             — Value object between stages
│   ├── StripeCheckoutSessionCreator.java    — Creates Stripe sessions
│   ├── StripeSessionResult.java             — Value object (sessionId + URL)
│   ├── StripeWebhookService.java            — Webhook coordinator (non-transactional)
│   ├── StripeWebhookBusinessProcessor.java  — Webhook business logic (transactional)
│   ├── StripeWebhookEventRecorder.java      — Event dedup coordinator
│   ├── StripeWebhookEventTransactionService.java — Event dedup transactions
│   └── PaymentStatusService.java            — Status polling + Stripe sync fallback
├── entity/
│   ├── Payment.java                  — JPA entity
│   ├── PaymentStatus.java           — Enum with isTerminal()
│   ├── PaymentProvider.java          — Enum (STRIPE only)
│   ├── StripeWebhookEvent.java       — JPA entity for dedup
│   └── WebhookEventStatus.java       — Enum (PROCESSING/PROCESSED/RETRYABLE_FAILED)
├── repository/
│   ├── PaymentRepository.java        — JPA repository with pessimistic locking
│   └── StripeWebhookEventRepository.java
├── converter/
│   └── StripeSessionLineItemListConverter.java — MapStruct cart→Stripe converter
└── exception/
    ├── StripeSessionCreationException.java
    ├── PaymentEventProcessingException.java
    └── handler/
        └── PaymentExceptionHandler.java
```

### Why So Many Classes?

Each class exists for a specific reason:

| Class | Why it exists |
|-------|--------------|
| `CheckoutPaymentService` | Coordinates the 3-stage checkout. Keeps Stripe API call outside DB transaction. |
| `CheckoutPaymentTransactionService` | Spring `@Transactional` only works through proxies. If `CheckoutPaymentService` had `@Transactional` methods and called them internally, the transactions would be silently skipped. This is the **Spring self-invocation trap**. |
| `StripeWebhookService` | Coordinates webhook processing without being transactional itself. |
| `StripeWebhookBusinessProcessor` | Separate `@Transactional` bean for the same self-invocation reason. |
| `StripeWebhookEventRecorder` | Coordinates event dedup (insert-first pattern). |
| `StripeWebhookEventTransactionService` | Each dedup operation needs `REQUIRES_NEW` propagation (independent transaction). Must be a separate bean for Spring proxy to work. |
| `PaymentStatusService` | Read-only status + Stripe sync fallback. Separate concern from checkout and webhooks. |

**Rule of thumb:** If you see two classes that look like they could be one, check if one has `@Transactional`. If yes, the split exists because Spring requires it.

### The Three REST Endpoints

```yaml
POST /api/v1/payment/checkout
  Headers: Idempotency-Key (required)
  Body: { recipientName, recipientSurname, recipientPhone, address or deliveryAddressId }
  Returns: { orderId, stripeSessionId, checkoutUrl }
  Auth: JWT required

GET /api/v1/payment/checkout/{orderId}/status
  Returns: { orderId, orderStatus, paymentStatus }
  Auth: JWT required (only your own orders)

POST /api/v1/payment/stripe/webhook
  Headers: Stripe-Signature (Stripe sends this)
  Body: raw JSON event payload
  Auth: No JWT — Stripe can't send JWT. Authenticated by signature verification.
```

The webhook endpoint is `permitAll()` in Spring Security because Stripe calls it server-to-server. Authentication is handled by verifying the webhook signature using the shared `STRIPE_WEBHOOK_SECRET`.

### Payment Status Lifecycle

```
CREATED
  → STRIPE_SESSION_CREATED     (after Stripe session is created)
    → AWAITING_ASYNC_CONFIRMATION (for async payment methods like bank transfers)
      → PAID                    (webhook confirms payment)
      → FAILED                  (async payment failed)
    → PAID                      (webhook confirms immediate card payment)
    → EXPIRED                   (Stripe session expired after 31 minutes)
    → RECONCILIATION_FAILED     (amount/currency mismatch between Stripe and local record)
  → PAID → REFUNDED             (after charge.refunded webhook)
```

Terminal statuses (`PAID`, `FAILED`, `EXPIRED`, `REFUNDED`, `RECONCILIATION_FAILED`) can never be overwritten by later webhook events. This is enforced by `PaymentStatus.isTerminal()`.

### Order Status Lifecycle (Payment-Related)

```
PENDING_PAYMENT
  → PAID                (payment confirmed)
  → PAYMENT_FAILED      (async payment failed)
  → PAYMENT_EXPIRED     (Stripe session expired)
  → CANCELLED           (user cancels before payment)
```

---

## Frontend Architecture

The frontend is intentionally simple. No Stripe JS SDK. No custom card form. Just a redirect to Stripe Hosted Checkout.

### File Structure

```
src/
├── features/
│   ├── payment/
│   │   ├── paymentApi.ts        — API client (createCheckout, getCheckoutStatus)
│   │   ├── config.ts            — Feature flag (NEXT_PUBLIC_STRIPE_ENABLED)
│   │   ├── public.ts            — Barrel re-export
│   │   └── CheckoutSuccess.tsx  — Success page polling component
│   └── checkout/
│       ├── useCheckoutForm.ts   — Form state + submission logic
│       ├── checkoutTypes.ts     — TypeScript interfaces
│       └── components/
│           ├── CheckoutForm.tsx  — Checkout page UI
│           └── CheckoutSummary.tsx — Order summary display
├── app/
│   └── checkout/
│       ├── page.tsx             — /checkout route (auth-gated)
│       ├── success/page.tsx     — /checkout/success route
│       └── cancel/page.tsx      — /checkout/cancel route
```

### How the Frontend Sends the Idempotency Key

The frontend generates a UUID and sends it as a header:

```typescript
// paymentApi.ts
const response = await api.post<CheckoutResponse>(
  '/payment/checkout',
  payload,
  { headers: { 'Idempotency-Key': idempotencyKey } },
)
```

The Next.js API proxy (`/api/proxy/[...path]`) forwards this header to the backend. The `FORWARDED_HEADERS` array includes `'Idempotency-Key'`.

### Why the Cart Is Not Cleared on Submit

This is a common mistake in payment integrations. If you clear the cart when the user clicks "Place order":

1. User clicks "Place order" → cart cleared
2. Stripe page loads → user closes the tab
3. User comes back → cart is empty, order is stuck at PENDING_PAYMENT

Instead, Iced Latte clears the cart only after the backend confirms payment via webhook. The frontend resets its local cart state only after polling confirms `orderStatus === 'PAID'`.

### The Feature Flag

```typescript
// config.ts
export const hostedCheckoutEnabled =
  process.env.NEXT_PUBLIC_STRIPE_ENABLED === 'true'
```

When disabled, the "Place order" button is grayed out and shows "Hosted checkout is disabled for this environment." This is explicit-only — if the env var is missing, checkout is disabled.

---

## Database Design

### payments table

```sql
CREATE TABLE public.payments (
    id                          UUID PRIMARY KEY,
    order_id                    UUID NOT NULL UNIQUE REFERENCES public.orders(id),
    user_id                     UUID NOT NULL,
    provider                    VARCHAR(20)  NOT NULL DEFAULT 'STRIPE',
    provider_session_id         VARCHAR(255) UNIQUE,
    provider_payment_intent_id  VARCHAR(255),
    status                      VARCHAR(30)  NOT NULL DEFAULT 'CREATED',
    amount_minor                BIGINT       NOT NULL,
    currency                    VARCHAR(3)   NOT NULL DEFAULT 'usd',
    raw_event_id                VARCHAR(255),
    latest_event_type           VARCHAR(100),
    checkout_idempotency_key    VARCHAR(100),
    created_at                  TIMESTAMPTZ DEFAULT current_timestamp,
    updated_at                  TIMESTAMPTZ
);
```

**Key design decisions:**
- `amount_minor` stores cents (e.g., $25.00 = 2500). This avoids floating-point rounding issues — standard practice in payment systems.
- `order_id` is UNIQUE — one payment per order.
- `provider_session_id` is UNIQUE — prevents duplicate Stripe sessions.
- `checkout_idempotency_key` + `user_id` has a unique partial index — prevents the same user from creating duplicate checkouts with the same key.

### stripe_webhook_events table

```sql
CREATE TABLE public.stripe_webhook_events (
    stripe_event_id  VARCHAR(255) PRIMARY KEY,
    event_type       VARCHAR(100) NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'PROCESSING',
    received_at      TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    processed_at     TIMESTAMPTZ,
    failure_reason   TEXT
);
```

Uses Stripe's event ID as the primary key. This is the **insert-first deduplication pattern**: try to INSERT the event; if it fails with a unique constraint violation, the event is a duplicate.

---

## Webhook Processing

### Why Webhooks?

When a user pays on Stripe's hosted page, your backend doesn't know about it immediately. Stripe sends a webhook — an HTTP POST to your server — to notify you. This is the standard pattern for all payment providers.

### The Webhook Flow

```
Stripe sends POST /api/v1/payment/stripe/webhook
  │
  ├─ 1. Verify signature (reject if invalid)
  │
  ├─ 2. Try to acquire event (insert-first dedup)
  │     ├─ New event → INSERT with PROCESSING status
  │     ├─ Duplicate, already PROCESSED → skip (return 200)
  │     ├─ Duplicate, RETRYABLE_FAILED → re-acquire for retry
  │     └─ Duplicate, still PROCESSING → skip (another instance is handling it)
  │
  ├─ 3. Process business logic (in a transaction)
  │     ├─ checkout.session.completed → mark PAID, transition order, clear cart
  │     ├─ checkout.session.expired → mark EXPIRED, transition order
  │     ├─ checkout.session.async_payment_failed → mark FAILED
  │     └─ charge.refunded → transition order to REFUNDED
  │
  ├─ 4a. Success → mark event PROCESSED
  │
  └─ 4b. Transient failure → mark event RETRYABLE_FAILED, rethrow
          (Stripe will retry because it got a non-200 response)
```

### Events We Listen To

| Event | When it fires | What we do |
|-------|--------------|------------|
| `checkout.session.completed` | User completed payment | Mark payment PAID, transition order, clear cart |
| `checkout.session.expired` | Session expired (31 min) | Mark payment EXPIRED, transition order |
| `checkout.session.async_payment_succeeded` | Async payment method confirmed | Same as completed |
| `checkout.session.async_payment_failed` | Async payment method failed | Mark payment FAILED |
| `charge.refunded` | Refund processed | Transition order from REFUND_REQUESTED → REFUNDED |

### Signature Verification

Every webhook request includes a `Stripe-Signature` header. The backend verifies it using the shared `STRIPE_WEBHOOK_SECRET`:

```java
// StripeWebhookService.parseEvent()
Event event = Webhook.constructEvent(payload, signature, webhookSecret);
```

If the signature is invalid, the request is rejected with 400. This prevents anyone from faking webhook events.

---

## Safety Mechanisms

### 1. Idempotency (Checkout)

If the user's browser sends the same checkout request twice (network retry, double-click), the backend returns the same order instead of creating a duplicate. This is enforced by the `checkout_idempotency_key` + `user_id` unique index.

### 2. Idempotency (Webhooks)

Stripe may send the same webhook event multiple times. The `stripe_webhook_events` table with its insert-first pattern ensures each event is processed exactly once.

### 3. Pessimistic Locking

When processing a webhook, the backend acquires a `PESSIMISTIC_WRITE` lock on the Payment row:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
Optional<Payment> findByOrderIdForUpdate(UUID orderId);
```

This prevents two concurrent webhook events (e.g., `completed` and `expired` arriving at the same time) from corrupting the payment state.

### 4. Terminal State Protection

Once a payment reaches a terminal status, no webhook can change it:

```java
// PaymentStatus.java
public boolean isTerminal() {
    return this == PAID || this == REFUNDED
        || this == RECONCILIATION_FAILED
        || this == FAILED || this == EXPIRED;
}
```

Example: if a payment is already `PAID`, a delayed `expired` webhook is silently ignored.

### 5. Reconciliation Guard

Before marking a payment as PAID, the backend verifies the amount and currency from Stripe match the local record:

```java
if (!stripeAmount.equals(payment.getAmountMinor())
        || !stripeCurrency.equalsIgnoreCase(payment.getCurrency())) {
    payment.setStatus(PaymentStatus.RECONCILIATION_FAILED);
    return; // Persisted, not thrown — TX commits normally
}
```

This catches tampering or bugs where the charged amount doesn't match what the backend expected.

### 6. Stripe Sync Fallback

The success page polling endpoint doesn't just read the database — if the webhook hasn't arrived yet, it calls `Session.retrieve()` to check Stripe directly:

```java
// PaymentStatusService.getStatus()
if (payment != null && !payment.getStatus().isTerminal()
        && payment.getProviderSessionId() != null) {
    trySyncFromStripe(payment);
}
```

This is the reconciliation fallback. Real payment systems never rely on a single delivery mechanism for money. The webhook is the primary path; the sync is the safety net.

### 7. Spring Transaction Boundary Separation

Spring's `@Transactional` uses proxy-based AOP. If a class calls its own `@Transactional` method, the proxy is bypassed and the transaction doesn't apply. This is the **self-invocation trap**.

Iced Latte solves this by extracting transactional methods into separate beans:

```
CheckoutPaymentService (non-TX coordinator)
  → CheckoutPaymentTransactionService (@Transactional methods)

StripeWebhookEventRecorder (non-TX coordinator)
  → StripeWebhookEventTransactionService (@Transactional REQUIRES_NEW methods)
```

This is not overengineering — it's a real Spring requirement for correct transaction behavior.

---

## Configuration

### Backend (.env)

```bash
# Stripe (TEST MODE ONLY — no real money is charged)
# Get test keys from: https://dashboard.stripe.com/test/apikeys
# Use Stripe test cards: https://docs.stripe.com/testing#cards
STRIPE_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...your_key...
STRIPE_WEBHOOK_SECRET=whsec_...your_key...

# Frontend URL (used for Stripe success/cancel redirect URLs)
FRONTEND_URL=http://localhost:3000
```

- `STRIPE_ENABLED=false` disables all payment endpoints (they won't even register as Spring beans)
- `STRIPE_SECRET_KEY` is the server-side secret key from Stripe Dashboard → API keys
- `STRIPE_WEBHOOK_SECRET` is the signing secret for verifying webhook signatures
- Never use `sk_live_...` keys. Only `sk_test_...`.

### Frontend (.env.local)

```bash
NEXT_PUBLIC_STRIPE_ENABLED=true
NEXT_PUBLIC_API_URL=http://localhost:8083/api/v1
NEXT_PUBLIC_FRONTEND_URL=http://localhost:3000
```

- `NEXT_PUBLIC_STRIPE_ENABLED=true` enables the checkout button. If missing or `false`, checkout is disabled.
- No Stripe publishable key is needed — the frontend never loads Stripe.js. It just redirects to the checkout URL returned by the backend.

---

## Local Development Setup

### Prerequisites

- Java 25, Maven 3.9+, Docker Desktop
- Node.js 20+, npm
- A free Stripe account (sandbox mode)

### Step 1 — Get Stripe Test Keys

1. Go to https://dashboard.stripe.com/test/apikeys
2. Copy the **Secret key** (`sk_test_...`)
3. Paste it into `Iced-Latte/.env` as `STRIPE_SECRET_KEY`
4. Set `STRIPE_ENABLED=true`

### Step 2 — Set the Webhook Secret

For local development, you have two options:

**Option A — Without Stripe CLI (simpler):**
Set `STRIPE_WEBHOOK_SECRET` to any placeholder value (e.g., `whsec_placeholder`). The webhook endpoint will reject events with invalid signatures, but the **sync fallback** in `PaymentStatusService` will still work — it calls Stripe directly when the success page polls. This is enough for local testing.

**Option B — With Stripe CLI (full webhook flow):**
1. Install Stripe CLI: `brew install stripe/stripe-cli/stripe`
2. Login: `stripe login`
3. Run: `stripe listen --forward-to localhost:8083/api/v1/payment/stripe/webhook`
4. Copy the `whsec_...` value it prints
5. Paste it into `.env` as `STRIPE_WEBHOOK_SECRET`
6. Restart the backend

### Step 3 — Start Everything

```bash
# Terminal 1: Backend
cd Iced-Latte
export $(cat .env | xargs) && mvn spring-boot:run

# Terminal 2: Frontend
cd Iced-Latte-Frontend
npm run dev

# Terminal 3 (optional): Stripe CLI
stripe listen --forward-to localhost:8083/api/v1/payment/stripe/webhook
```

### Step 4 — Test the Flow

1. Open http://localhost:3000
2. Sign in (or register)
3. Add items to cart
4. Go to cart → "Go to checkout"
5. Fill in recipient info and address
6. Click "Place order"
7. On Stripe's page, use test card `4242 4242 4242 4242`
8. Any future expiry, any CVC, any postcode
9. Click Pay
10. You'll be redirected to the success page
11. The success page polls the backend and shows ✅ when payment is confirmed

---

## Testing with Stripe Test Cards

| Card Number | Behavior |
|-------------|----------|
| `4242 4242 4242 4242` | Succeeds immediately |
| `4000 0000 0000 3220` | Requires 3D Secure authentication |
| `4000 0000 0000 0002` | Declined |
| `4000 0000 0000 9995` | Insufficient funds |
| `4000 0025 0000 3155` | Requires authentication, then succeeds |

For all test cards: use any future expiry date, any 3-digit CVC, and any valid postcode.

Full list: https://docs.stripe.com/testing#cards

> **Never use real card numbers.** Stripe test mode only accepts test cards. Real card numbers will be rejected.

---

## Glossary

| Term | Meaning |
|------|---------|
| **Stripe Hosted Checkout** | A Stripe-hosted payment page. The user is redirected there — your app never sees card details. |
| **Checkout Session** | A Stripe object representing a single checkout attempt. Contains line items, prices, and redirect URLs. Created by the backend via Stripe API. |
| **Payment Intent** | A Stripe object representing the actual charge. Created automatically by the Checkout Session. Used for refund lookup. |
| **Webhook** | An HTTP POST that Stripe sends to your server when something happens (payment completed, expired, refunded, etc.). |
| **Webhook Signing Secret** | A shared secret (`whsec_...`) used to verify that a webhook really came from Stripe, not an attacker. |
| **Idempotency Key** | A unique identifier sent with a request to ensure it's processed at most once. If the same key is sent again, the server returns the previous result instead of creating a duplicate. |
| **Terminal Status** | A payment status that cannot be changed by later events (PAID, FAILED, EXPIRED, REFUNDED, RECONCILIATION_FAILED). |
| **Pessimistic Lock** | A database lock that prevents other transactions from reading/writing the same row until the lock is released. Used to prevent race conditions between concurrent webhook events. |
| **REQUIRES_NEW** | A Spring transaction propagation mode that creates a new independent transaction, suspending any existing one. Used for webhook event dedup so the event status is committed regardless of whether the business logic succeeds or fails. |
| **Self-invocation trap** | A Spring gotcha where calling a `@Transactional` method from within the same class bypasses the proxy, so the transaction annotation is silently ignored. Fixed by extracting the method into a separate bean. |
| **Reconciliation** | Verifying that the amount Stripe charged matches what your backend expected. Catches bugs or tampering. |
| **Sync fallback** | When the webhook hasn't arrived, the backend calls Stripe's API directly (`Session.retrieve()`) to check payment status. This is the safety net — real payment systems always have one. |

---

*This guide was written for Iced Latte v4.0.5 (May 2026). Stripe API version: 2025-04-30.basset.*

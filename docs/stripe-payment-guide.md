# Stripe Payment Integration — System Design Guide

A deep-dive into how Iced Latte integrates with Stripe for payment processing.
Written for engineers preparing for senior-level system design interviews.

> **Test mode only.** Iced Latte uses Stripe sandbox. No real money is charged.

---

## Table of Contents

1. [Why This Guide Exists](#why-this-guide-exists)
2. [Architecture Overview](#architecture-overview)
3. [Why Stripe Hosted Checkout](#why-stripe-hosted-checkout)
4. [The Payment Flow Step by Step](#the-payment-flow-step-by-step)
5. [The Saga Pattern — Checkout as a Distributed Transaction](#the-saga-pattern--checkout-as-a-distributed-transaction)
6. [Idempotency — Seven Layers Deep](#idempotency--seven-layers-deep)
7. [Webhook Processing — Exactly-Once Semantics](#webhook-processing--exactly-once-semantics)
8. [State Machines — Payment and Order Lifecycles](#state-machines--payment-and-order-lifecycles)
9. [Locking Strategies — Pessimistic vs Optimistic](#locking-strategies--pessimistic-vs-optimistic)
10. [Transaction Boundary Design](#transaction-boundary-design)
11. [Eventual Consistency and Reconciliation](#eventual-consistency-and-reconciliation)
12. [Failure Modes and Recovery](#failure-modes-and-recovery)
13. [Security Architecture](#security-architecture)
14. [Frontend Architecture — BFF Proxy and Polling](#frontend-architecture--bff-proxy-and-polling)
15. [Database Design](#database-design)
16. [Backend Package Structure](#backend-package-structure)
17. [Design Patterns Catalogue](#design-patterns-catalogue)
18. [Scaling Considerations](#scaling-considerations)
19. [Interview Cheat Sheet](#interview-cheat-sheet)
20. [Configuration Reference](#configuration-reference)
21. [Local Development Setup](#local-development-setup)
22. [Test Cards](#test-cards)
23. [Glossary](#glossary)

---

## Why This Guide Exists

Payment integration is one of the most common system design interview topics for senior engineers. It touches distributed systems, consistency, idempotency, state machines, security, and failure handling — all in one feature.

This guide uses Iced Latte's real, working Stripe integration to explain these concepts. Every code example comes from the actual codebase. Every tradeoff was made deliberately.

After reading this guide you should be able to:

- Design a payment integration from scratch in a system design interview
- Explain why webhooks exist and how to process them safely
- Discuss idempotency, exactly-once semantics, and saga patterns with concrete examples
- Identify failure modes and explain recovery strategies
- Compare Stripe Hosted Checkout vs Elements vs PaymentIntents and justify a choice

---

## Architecture Overview

Iced Latte owns the business logic. Stripe is the external payment rail.

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────┐
│   Browser    │     │  Iced Latte     │     │   Stripe    │
│  (Next.js)   │     │  Backend        │     │   (test)    │
│              │     │  (Spring Boot)  │     │             │
└──────┬───────┘     └────────┬────────┘     └──────┬──────┘
       │                      │                      │
       │  1. POST /checkout   │                      │
       │  + Idempotency-Key   │                      │
       │─────────────────────>│                      │
       │                      │                      │
       │                      │  2. TX A: Create     │
       │                      │  Order + Payment     │
       │                      │                      │
       │                      │  3. Stripe API:      │
       │                      │  Create Session      │
       │                      │  (outside TX)        │
       │                      │─────────────────────>│
       │                      │                      │
       │                      │  4. TX B: Save       │
       │                      │  session ID          │
       │                      │<─────────────────────│
       │                      │                      │
       │  5. Return checkout  │                      │
       │     URL              │                      │
       │<─────────────────────│                      │
       │                      │                      │
       │  6. Redirect to      │                      │
       │     Stripe page      │                      │
       │─────────────────────────────────────────────>│
       │                      │                      │
       │                      │  7. Webhook:         │
       │  8. Redirect back    │  session.completed   │
       │<─────────────────────│<─────────────────────│
       │                      │                      │
       │  9. Poll status      │  9. Update to PAID   │
       │─────────────────────>│     + clear cart     │
       │                      │                      │
       │  10. Show ✅          │                      │
       │<─────────────────────│                      │
```

**Key principle:** The backend creates the order and payment record *before* calling Stripe. Stripe only collects card details and processes the charge. The backend learns about the result through webhooks (primary) or by polling Stripe directly (fallback).

This is the **backend-owned order lifecycle** pattern. The alternative — letting Stripe own the order — creates a dependency on Stripe's data model and makes reconciliation harder.

---

## Why Stripe Hosted Checkout

Stripe offers three integration approaches. Understanding the tradeoffs is a common interview question.

### The Three Options

| Approach | How it works | PCI scope | Frontend complexity | Customization |
|----------|-------------|-----------|-------------------|---------------|
| **Hosted Checkout** (what we use) | Full-page redirect to Stripe's page | SAQ-A (minimal) | Zero Stripe JS | Low — Stripe controls the UI |
| **Stripe Elements** | Embedded card form via Stripe.js | SAQ-A-EP | Medium — load Stripe.js, mount Elements, handle events | High — custom styling |
| **PaymentIntents direct** | Build your own form, tokenize via Stripe.js | SAQ-A-EP | High — manage PaymentIntent lifecycle, 3DS callbacks | Full control |

### Why We Chose Hosted Checkout

1. **PCI SAQ-A compliance.** Card data never touches our frontend or backend. No card input fields, no Stripe.js SDK, no tokenization code. This is the simplest PCI compliance level — no quarterly ASV scans needed for card handling.

2. **Zero frontend Stripe dependencies.** The frontend just does `window.location.href = checkoutUrl`. No `@stripe/react-stripe-js`, no `loadStripe()`, no `Elements` provider, no `CardElement`. The entire Stripe interaction is a redirect.

3. **Security by design.** Zero card-related JavaScript means zero attack surface for XSS-based card skimming. An attacker who compromises the frontend cannot steal card data because card data never exists on our domain.

4. **Educational clarity.** The redirect-based flow makes the separation between "our code" and "Stripe's code" obvious. There's no ambiguity about where card processing happens.

### What We Give Up

- **No custom payment form styling.** The Stripe checkout page looks like Stripe, not like Iced Latte.
- **Full-page redirect.** The user leaves our site and comes back. With Elements, they stay on our page.
- **Less control over the payment flow.** We can't add custom fields to the payment form or implement split payments.
- **Stripe controls the UX.** We can't A/B test the payment form or add trust badges.

### When to Choose Each

- **Hosted Checkout:** MVP, educational projects, low-volume B2C, when PCI simplicity matters most.
- **Elements:** Production e-commerce where brand consistency matters but you still want Stripe to handle card security.
- **PaymentIntents direct:** Complex flows like marketplace payouts, subscriptions with metered billing, or multi-step checkout with custom 3DS handling.

> **Interview tip:** If asked "how would you design a payment system," start with Hosted Checkout and explain what would trigger a migration to Elements. This shows you understand the tradeoff spectrum.

---

## The Payment Flow Step by Step

### Step 1 — User clicks "Place order"

The frontend generates a UUID idempotency key and calls the backend:

```typescript
// useCheckoutForm.ts
const idempotencyKey = crypto.randomUUID()
const checkout = await createCheckout(
  { recipientName, recipientSurname, recipientPhone, address },
  idempotencyKey,
)
```

The cart is **not** cleared here. If the user closes the Stripe tab, the cart must still be intact.

### Step 2 — Backend runs Transaction A (Order + Payment creation)

```java
// CheckoutPaymentTransactionService.prepareCheckout() — @Transactional
Order order = orderCreator.createPendingPaymentOrder(userId, request, cart);
Payment payment = Payment.builder()
    .orderId(order.getId())
    .status(PaymentStatus.CREATED)
    .amountMinor(toMinorUnits(order.getItemsTotalPrice()))
    .currency("usd")
    .checkoutIdempotencyKey(idempotencyKey)
    .build();
```

**Why create the order before calling Stripe?** If the Stripe call fails, you still have a local record. You can retry without losing data. The order is a snapshot of the cart at checkout time — prices, quantities, product names are all frozen.

**Snapshot semantics:** Cart items are copied into `OrderItem` entities. Even if the user modifies the cart or a product price changes, the order reflects what was in the cart at checkout time. This is standard in e-commerce — you don't want a price change to affect an in-flight order.

### Step 3 — Backend calls Stripe API (outside any transaction)

```java
// CheckoutPaymentService.checkout() — NOT @Transactional
CheckoutPreparation prepared = txService.prepareCheckout(...);  // TX A
StripeSessionResult stripeResult = stripeSessionCreator.create(...);  // No TX
txService.saveStripeDetails(prepared.payment().getId(), stripeResult);  // TX B
```

**Why outside the transaction?** This is a critical design decision. The Stripe API call is a network round-trip (100-500ms). Holding a database transaction open during a network call:

- Blocks a DB connection from the pool for the entire duration
- Risks transaction timeout if Stripe is slow
- Prevents other threads from using that connection
- Under load, can exhaust the connection pool and bring down the entire application

**Rule:** Never hold a database transaction open during an external API call.

### Step 4 — Backend runs Transaction B (save Stripe session ID)

```java
// CheckoutPaymentTransactionService.saveStripeDetails() — @Transactional
payment.setProviderSessionId(stripeResult.sessionId());
payment.setStatus(PaymentStatus.STRIPE_SESSION_CREATED);
```

### Step 5 — Frontend redirects to Stripe

```typescript
window.location.href = checkout.checkoutUrl  // Full-page redirect
```

### Step 6 — User pays on Stripe's hosted page

The user enters test card `4242 4242 4242 4242`. Stripe processes the payment.

### Step 7 — Stripe sends webhook to backend

Stripe sends `POST /api/v1/payment/stripe/webhook` with a `checkout.session.completed` event. The backend verifies the signature, deduplicates the event, and updates the payment and order status to PAID.

### Step 8 — Stripe redirects browser to success page

The browser is redirected to `/checkout/success?order_id=...`.

### Step 9 — Success page polls for status

```typescript
// CheckoutSuccess.tsx — polls every 2s, max 5 retries
const result = await getCheckoutStatus(orderId)
if (result.orderStatus === 'PAID') {
  setStatus('paid')
  resetCart()  // Clear frontend cart only after confirmed payment
}
```

**Primary path:** Webhook already updated the status. First poll returns PAID.

**Fallback path:** If webhook hasn't arrived (common in local dev), the backend calls `Session.retrieve()` to check Stripe directly. This is the reconciliation fallback — real payment systems never rely on a single delivery mechanism for money.

---

## The Saga Pattern — Checkout as a Distributed Transaction

### What Is a Saga?

A saga is a sequence of local transactions where each step has a compensating action. If step N fails, you run compensating actions for steps N-1, N-2, ... back to step 1. Sagas replace distributed transactions (2PC/XA) in microservice architectures.

### Our Checkout Is a Saga

The checkout flow spans two data stores: PostgreSQL (local) and Stripe (remote). There's no distributed transaction coordinator. Instead, we use a saga:

```
TX A: Create Order(PENDING_PAYMENT) + Payment(CREATED)
  ↓
Stripe API: Create Checkout Session
  ↓
TX B: Save session ID, set Payment(STRIPE_SESSION_CREATED)
  ↓
[User pays on Stripe]
  ↓
Webhook TX: Set Payment(PAID), transition Order(PAID), delete cart
```

### Orchestration vs Choreography

There are two saga styles:

| Style | How it works | Our usage |
|-------|-------------|-----------|
| **Orchestration** | A central coordinator sequences the steps | Checkout flow — `CheckoutPaymentService` is the orchestrator |
| **Choreography** | Services react to events independently | Webhook flow — Stripe publishes events, our handler reacts |

**Iced Latte uses both.** The checkout is orchestrated (the service explicitly calls TX A → Stripe → TX B). The post-payment flow is choreographed (Stripe fires events, our webhook handler reacts to each event type independently).

### Compensating Actions

| Failure | Compensation |
|---------|-------------|
| Stripe session expires (user abandons) | `checkout.session.expired` webhook → Payment(EXPIRED), Order(PAYMENT_EXPIRED) |
| Async payment fails (bank transfer rejected) | `checkout.session.async_payment_failed` → Payment(FAILED), Order(PAYMENT_FAILED) |
| Amount mismatch detected | Payment(RECONCILIATION_FAILED) — requires human investigation, no auto-compensation |
| App crashes between TX A and Stripe call | Idempotent retry rebuilds from Order.items snapshot |
| App crashes between Stripe call and TX B | Idempotent retry detects missing session ID, retries Stripe (Stripe deduplicates) |

### What's Missing for a Full Saga

There's no inventory reservation. Cart items are snapshot into the order but not reserved. If two users buy the last item simultaneously, both orders succeed. This is acceptable for a marketplace with abundant inventory but would need saga compensation (release reserved stock on payment failure) for scarce inventory.

> **Interview tip:** When discussing sagas, always mention compensating actions and what happens when compensation itself fails. In our case, RECONCILIATION_FAILED is a terminal state that requires human intervention — there's no automatic recovery for an amount mismatch.

---

## Idempotency — Seven Layers Deep

Idempotency means "doing the same thing twice produces the same result." In payment systems, this is critical — you never want to charge a customer twice.

### Layer 1 — Client-Generated Idempotency Key

```typescript
const idempotencyKey = crypto.randomUUID()
```

The frontend generates a UUID per checkout attempt and sends it as an `Idempotency-Key` HTTP header. If the browser retries (network timeout, double-click), the same key is sent.

### Layer 2 — Application-Level Check

```java
Payment existing = paymentRepository
    .findByCheckoutIdempotencyKeyAndUserId(idempotencyKey, userId)
    .orElse(null);
if (existing != null) {
    return resolveExistingCheckout(prepared, user.getEmail());
}
```

The backend checks if this key was already used by this user. If so, it returns the existing order instead of creating a new one.

### Layer 3 — Database Unique Index

```sql
CREATE UNIQUE INDEX idx_payments_user_checkout_idempotency
    ON payments (user_id, checkout_idempotency_key)
    WHERE checkout_idempotency_key IS NOT NULL;
```

Even if two concurrent requests pass the application-level check (TOCTOU race), the database unique index prevents duplicate inserts. One request wins, the other gets a constraint violation.

**Why a partial index?** The `WHERE checkout_idempotency_key IS NOT NULL` clause excludes rows without an idempotency key from the index. This keeps the index small and allows legacy payments (if any) without keys.

### Layer 4 — Stripe-Level Idempotency

```java
RequestOptions options = RequestOptions.builder()
    .setIdempotencyKey("checkout-session:" + orderId)
    .build();
Session session = Session.create(params, options);
```

Stripe itself deduplicates. If the same `orderId` is sent twice, Stripe returns the same session. This is the third line of defense — even if our application creates duplicate Payment records (shouldn't happen, but defense in depth), Stripe won't create duplicate charges.

### Layer 5 — Webhook Event Deduplication

```java
// StripeWebhookEventTransactionService.tryInsertNewEvent()
StripeWebhookEvent event = StripeWebhookEvent.builder()
    .stripeEventId(eventId)  // Stripe's globally unique event ID
    .status(WebhookEventStatus.PROCESSING)
    .build();
webhookEventRepository.save(event);  // PK constraint = dedup
```

Stripe may send the same webhook event multiple times (at-least-once delivery). The `stripe_webhook_events` table uses Stripe's event ID as the primary key. Attempting to INSERT a duplicate throws a constraint violation → event is skipped.

This is the **insert-first deduplication pattern**: try to insert, catch the constraint violation. It's simpler and more race-condition-safe than the alternative (SELECT then INSERT, which has a TOCTOU gap).

### Layer 6 — Terminal State Guard

```java
// PaymentStatus.java
public boolean isTerminal() {
    return this == PAID || this == REFUNDED
        || this == RECONCILIATION_FAILED
        || this == FAILED || this == EXPIRED;
}

// StripeWebhookBusinessProcessor.markPaid()
if (payment.getStatus().isTerminal()) {
    log.info("payment.webhook.skipped_terminal: ...");
    return;
}
```

Even if a duplicate event somehow passes the event dedup layer, the terminal state guard prevents re-processing. Once a payment is PAID, no webhook can change it.

### Layer 7 — Pessimistic Lock on Payment Row

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
Optional<Payment> findByOrderIdForUpdate(UUID orderId);
```

Different Stripe events (e.g., `checkout.session.completed` and `checkout.session.async_payment_succeeded`) can target the same order with different event IDs. Event-level dedup (Layer 5) won't catch this because the event IDs are different. The pessimistic lock serializes all webhook processing for the same order.

### Why So Many Layers?

Each layer catches a different failure mode:

| Layer | What it catches |
|-------|----------------|
| Client key | Browser retries, double-clicks |
| App check | Normal duplicate requests |
| DB index | Race conditions (concurrent requests) |
| Stripe key | Duplicate Stripe sessions |
| Event dedup | Stripe webhook retries (same event ID) |
| Terminal guard | Different events for same order after completion |
| Pessimistic lock | Concurrent events for same order (different event IDs) |

> **Interview tip:** When asked about idempotency, don't just say "use an idempotency key." Explain that you need defense in depth — application check, database constraint, and provider-level dedup. Each catches a different class of failure.

---

## Webhook Processing — Exactly-Once Semantics

### Why Webhooks Exist

When a user pays on Stripe's hosted page, your backend doesn't know about it immediately. The browser redirect is unreliable — the user might close the tab. Stripe sends a webhook (HTTP POST to your server) as the authoritative notification.

### Delivery Semantics

| Direction | Guarantee | Mechanism |
|-----------|-----------|-----------|
| Stripe → Our backend | At-least-once | Stripe retries on non-200 for up to 3 days |
| Our processing | Effectively-once | Event dedup + terminal guard + pessimistic lock |
| Our backend → Stripe | At-most-once | Stripe's own idempotency key on session creation |

**At-least-once → effectively-once conversion** is the core challenge. Stripe guarantees delivery but may deliver multiple times. Our deduplication layers convert this to effectively-once business processing.

### The Webhook Processing Pipeline

```
POST /api/v1/payment/stripe/webhook
  │
  ├─ 1. Parse + verify signature
  │     Stripe-Signature header verified against STRIPE_WEBHOOK_SECRET
  │     Invalid → 400 (reject immediately, don't record)
  │
  ├─ 2. Acquire event (insert-first dedup)
  │     INSERT stripe_webhook_events (stripe_event_id, status=PROCESSING)
  │     ├─ Success → new event, proceed
  │     ├─ PK violation + status=PROCESSED → duplicate, return 200
  │     ├─ PK violation + status=RETRYABLE_FAILED → re-acquire for retry
  │     └─ PK violation + status=PROCESSING → another instance handling it, return 200
  │
  ├─ 3. Process business logic (@Transactional)
  │     SELECT payment FOR UPDATE (pessimistic lock)
  │     Check terminal state guard
  │     Verify amount/currency (reconciliation)
  │     Update Payment + Order status
  │     Delete shopping cart
  │
  ├─ 4a. Success → mark event PROCESSED (REQUIRES_NEW TX)
  │
  └─ 4b. Transient failure → mark event RETRYABLE_FAILED (REQUIRES_NEW TX)
          Rethrow exception → Stripe gets non-200 → retries
```

### Events We Handle

| Stripe Event | When | What We Do |
|-------------|------|------------|
| `checkout.session.completed` | User completed payment | Verify amount → PAID, transition order, clear cart |
| `checkout.session.expired` | 31-minute session timeout | EXPIRED, transition order to PAYMENT_EXPIRED |
| `checkout.session.async_payment_succeeded` | Bank transfer confirmed | Same as completed |
| `checkout.session.async_payment_failed` | Bank transfer rejected | FAILED, transition order to PAYMENT_FAILED |
| `charge.refunded` | Refund processed | Transition order from REFUND_REQUESTED → REFUNDED |

### Error Classification — Retryable vs Non-Retryable

This is a critical design decision. The webhook handler distinguishes between two types of failures:

**Non-retryable (business failures):**
- Amount mismatch → persist RECONCILIATION_FAILED, return 200 to Stripe
- Invalid order state → log warning, return 200
- These are persisted and acknowledged. Retrying won't fix them.

**Retryable (transient failures):**
- Database timeout → mark event RETRYABLE_FAILED, rethrow → Stripe gets 500 → retries
- Connection pool exhausted → same
- These might succeed on retry.

```java
// StripeWebhookService.processEvent()
try {
    businessProcessor.process(event);
    eventRecorder.markProcessed(eventId);
} catch (Exception e) {
    eventRecorder.markRetryableFailed(eventId, e.getMessage());
    throw e;  // Stripe gets non-200, will retry
}
```

**Why non-retryable failures return 200:** If you return 500 for an amount mismatch, Stripe will retry the same event for 3 days. The mismatch won't fix itself. You'd get thousands of retries for nothing. Instead, persist the failure status and return 200 so Stripe stops retrying.

### The REQUIRES_NEW Pattern

Event status updates use `@Transactional(propagation = REQUIRES_NEW)`:

```java
// StripeWebhookEventTransactionService
@Transactional(propagation = Propagation.REQUIRES_NEW)
public boolean tryInsertNewEvent(String eventId, String eventType) { ... }

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void markProcessed(String eventId) { ... }

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void markRetryableFailed(String eventId, String reason) { ... }
```

**Why REQUIRES_NEW?** Each event status change must commit independently of the business transaction. If the business TX rolls back (transient failure), the event must still be marked RETRYABLE_FAILED — otherwise the next retry would think it's a new event and skip the dedup check.

REQUIRES_NEW suspends the outer transaction, creates a new one, commits it, then resumes the outer transaction. This guarantees the event status is persisted regardless of what happens to the business logic.

> **Interview tip:** When discussing webhook processing, emphasize the error classification. "We return 200 for non-retryable failures to prevent infinite retry loops, and 500 for transient failures so the provider retries. The event dedup table tracks which events have been processed, with REQUIRES_NEW transactions to ensure the tracking survives business logic failures."

---

## State Machines — Payment and Order Lifecycles

### Payment Status State Machine

```
CREATED ──────────────────────────────────────────────────────┐
  │                                                           │
  ▼                                                           │
STRIPE_SESSION_CREATED ───────────────────────────────┐       │
  │                    │              │                │       │
  │                    ▼              ▼                ▼       │
  │    AWAITING_ASYNC_CONFIRMATION   PAID*   RECONCILIATION_  │
  │         │              │                   FAILED*        │
  │         ▼              ▼                                  │
  │       PAID*         FAILED*                               │
  │         │                                                 │
  │         ▼                                                 │
  │      REFUNDED*                                            │
  │                                                           │
  └──────────────────► EXPIRED*  ◄────────────────────────────┘

  * = terminal state (isTerminal() returns true)
```

**Terminal states are immutable.** Once a payment reaches PAID, FAILED, EXPIRED, REFUNDED, or RECONCILIATION_FAILED, no webhook can change it. This is enforced by `PaymentStatus.isTerminal()` — checked at the top of every webhook handler.

**Why RECONCILIATION_FAILED is terminal:** An amount mismatch between Stripe and our database is a serious integrity issue. It could indicate tampering, a bug, or a race condition with price changes. Auto-recovery would be dangerous — it requires human investigation.

### Order Status State Machine

```java
// OrderStatusTransitioner.TRANSITIONS — explicit transition map
PENDING_PAYMENT ──PENDING_PAYMENT_CONFIRMED──→ PAID
                ──PAYMENT_FAILED_EVENT──────→ PAYMENT_FAILED
                ──PAYMENT_EXPIRED_EVENT─────→ PAYMENT_EXPIRED
                ──CANCEL────────────────────→ CANCELLED

PAID ───────────SHIP────────────────────────→ SHIPPED
                ──CANCEL────────────────────→ CANCELLED
                ──REQUEST_REFUND────────────→ REFUND_REQUESTED

SHIPPED ────────DELIVER─────────────────────→ DELIVERED

REFUND_REQUESTED ──REFUND_CONFIRMED─────────→ REFUNDED
```

The transition map is a `Map<OrderStatus, Map<OrderEvent, OrderStatus>>`. Invalid transitions throw `InvalidOrderStateTransitionException`. This is an **explicit state machine** — every valid transition is enumerated. There's no default or fallback behavior.

**Guards on transitions:**
- `CANCEL` checks `cancellationDeadline` — time-based guard
- `REQUEST_REFUND` checks `actorId == order.userId` — authorization guard

**Domain events:** Every transition publishes `OrderStatusChangedEvent` via Spring's `ApplicationEventPublisher`. Other modules can react without the order module knowing about them (loose coupling).

### Why Two Separate State Machines?

Payment and Order have different lifecycles and different owners:

- **Payment** tracks what happened with the money (Stripe's perspective)
- **Order** tracks what happened with the business process (our perspective)

A payment can be PAID while the order is still being SHIPPED. A payment can be REFUNDED while the order transitions through REFUND_REQUESTED → REFUNDED. Merging them into one state machine would create a combinatorial explosion of states.

---

## Locking Strategies — Pessimistic vs Optimistic

### The Two Approaches

| Strategy | How it works | When to use |
|----------|-------------|-------------|
| **Pessimistic** (`SELECT FOR UPDATE`) | Lock the row before reading. Other transactions wait. | High write contention, concurrent updates expected |
| **Optimistic** (`@Version`) | Read without locking. On write, check version hasn't changed. Retry on conflict. | Low contention, reads >> writes |

### What We Use and Why

| Entity | Strategy | Rationale |
|--------|----------|-----------|
| **Payment** | Pessimistic (`findByOrderIdForUpdate`) | Multiple webhook events can target the same order simultaneously. `completed` + `async_payment_succeeded` might arrive at the same time with different event IDs. Pessimistic locking serializes them — one waits for the other. |
| **Order** | Optimistic (`@Version`) | Order status transitions are sequential in normal flow. Concurrent transitions are rare. Optimistic locking avoids the overhead of row-level locks for the common case. |
| **StripeWebhookEvent** | Insert-first (PK constraint) | Not traditional locking. The PRIMARY KEY constraint acts as a mutex — only one INSERT succeeds, duplicates get a constraint violation. |

### Why Not Optimistic for Payment?

Optimistic locking throws `OptimisticLockException` on conflict, requiring application-level retry logic. For webhooks, this would mean:

1. Webhook A reads Payment (version 1)
2. Webhook B reads Payment (version 1)
3. Webhook A writes (version 1 → 2) ✓
4. Webhook B writes (version 1 → 2) ✗ OptimisticLockException
5. Webhook B must retry — but it's a webhook handler, not a user request

The retry would need to re-read, re-validate, and re-process. With pessimistic locking, Webhook B simply waits for A to finish, then proceeds with the updated state. Simpler and more predictable.

### Why Not Pessimistic for Order?

Order transitions happen in response to user actions (cancel, request refund) or webhook events (payment confirmed). These are rarely concurrent for the same order. Pessimistic locking would acquire a row lock on every order read, adding unnecessary overhead. Optimistic locking is cheaper for the common case and handles the rare conflict gracefully.

> **Interview tip:** "We use pessimistic locking for payments because webhooks create genuine write contention — multiple events for the same order can arrive simultaneously. We use optimistic locking for orders because transitions are sequential in normal flow. The choice depends on the concurrency profile of each entity."

---

## Transaction Boundary Design

### The Spring Self-Invocation Trap

Spring's `@Transactional` uses proxy-based AOP. When you call a `@Transactional` method from within the same class, the call goes directly to the method (bypassing the proxy), so the transaction annotation is silently ignored.

```java
// BROKEN — self-invocation bypasses proxy
@Service
public class PaymentService {
    public void checkout() {
        prepareCheckout();  // ← Direct call, NOT through proxy. No transaction!
    }

    @Transactional
    public void prepareCheckout() { ... }
}
```

Iced Latte uses two solutions:

**Solution 1 — Separate bean (used for checkout and webhooks):**
```java
// CheckoutPaymentService (non-TX coordinator)
//   → calls CheckoutPaymentTransactionService (TX methods)
// StripeWebhookEventRecorder (non-TX coordinator)
//   → calls StripeWebhookEventTransactionService (REQUIRES_NEW methods)
```

**Solution 2 — TransactionTemplate (used for status polling):**
```java
// PaymentStatusService.syncPaidStatus()
transactionTemplate.executeWithoutResult(status -> {
    Payment locked = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
    // ... update payment and order ...
});
```

Both are valid. Separate beans are clearer for complex flows. TransactionTemplate is simpler for single methods.

### Transaction Boundary Map

| Operation | TX Strategy | Why |
|-----------|-------------|-----|
| `prepareCheckout()` | `@Transactional` | Must atomically create Order + Payment |
| Stripe API call | **No TX** | Network call — never hold DB TX during I/O |
| `saveStripeDetails()` | `@Transactional` | Short write, separate TX from preparation |
| Webhook event insert | `REQUIRES_NEW` | Must commit independently of business TX |
| Webhook business logic | `@Transactional` | Must atomically update Payment + Order + delete cart |
| Webhook event status update | `REQUIRES_NEW` | Must commit even if business TX rolls back |
| Status sync fallback | `TransactionTemplate` | Programmatic TX to avoid self-invocation |

### Why Three Transactions for Checkout?

The checkout flow uses TX A → No TX → TX B instead of one big transaction:

```
TX A: Create Order + Payment (commit)
      ↓
No TX: Call Stripe API (100-500ms network call)
      ↓
TX B: Save Stripe session ID (commit)
```

**If we used one transaction:** The DB connection would be held for 100-500ms during the Stripe call. With a connection pool of 10 and 20 concurrent checkouts, you'd exhaust the pool and all other database operations would block.

**The tradeoff:** There's a failure window between TX A and TX B. If the app crashes after TX A but before TX B, the Order and Payment exist but have no Stripe session ID. The idempotent retry path handles this — it detects the missing session ID and retries the Stripe call.

---

## Eventual Consistency and Reconciliation

### The Consistency Model

The system spans two data stores: PostgreSQL (local) and Stripe (remote). There is no distributed transaction. The system is **eventually consistent**.

```
Timeline:
  t0: TX A commits (Order=PENDING_PAYMENT, Payment=CREATED)
  t1: Stripe session created
  t2: TX B commits (Payment=STRIPE_SESSION_CREATED)
  t3: User pays on Stripe
  t4: Stripe fires webhook
  t5: Webhook TX commits (Payment=PAID, Order=PAID)

Between t3 and t5, Stripe knows the payment succeeded but our DB doesn't.
This is the consistency window.
```

### CAP Theorem Implications

The system prioritizes **Availability** and **Partition tolerance** over strong Consistency (AP in CAP terms):

- **Availability:** The checkout endpoint returns immediately after TX A + Stripe call + TX B. It doesn't wait for payment confirmation.
- **Partition tolerance:** If the network between our app and Stripe is partitioned, the webhook won't arrive. The polling fallback (`Session.retrieve()`) provides eventual reconciliation.
- **Consistency:** Eventual, not strong. During the consistency window, our DB shows PENDING_PAYMENT while Stripe shows PAID.

### Three Reconciliation Mechanisms

**1. Webhook (primary):** Stripe pushes state changes to our backend. This is the fastest path — typically arrives within seconds of payment.

**2. Polling fallback (on-demand):** When the success page polls `GET /checkout/{orderId}/status` and the payment is still pending, the backend calls `Session.retrieve()` to check Stripe directly:

```java
// PaymentStatusService.trySyncFromStripe()
Session session = Session.retrieve(payment.getProviderSessionId());
if ("paid".equals(session.getPaymentStatus())) {
    syncPaidStatus(payment.getOrderId(), session);
}
```

This eliminates the need for Stripe CLI in local development. The sync fallback runs the same reconciliation guard (amount/currency verification) as the webhook path.

**3. Scheduled reconciliation (not yet implemented):** A production system should have a background job that periodically scans non-terminal payments older than N minutes and reconciles with Stripe. The polling fallback only runs when a user polls — a background job catches all cases.

### The Reconciliation Guard

Both the webhook and the sync fallback verify that Stripe's amount and currency match our local record:

```java
if (!stripeAmount.equals(payment.getAmountMinor())
        || !stripeCurrency.equalsIgnoreCase(payment.getCurrency())) {
    payment.setStatus(PaymentStatus.RECONCILIATION_FAILED);
    return;  // Persisted, not thrown — TX commits normally
}
```

**Why persist instead of throw?** Throwing inside a `@Transactional` method would roll back the transaction, losing the RECONCILIATION_FAILED status. By persisting and returning normally, the transaction commits and the failure is recorded for investigation.

> **Interview tip:** "Our system is eventually consistent between the local database and Stripe. We have three reconciliation mechanisms: webhooks as the primary path, on-demand polling as a fallback, and a scheduled job for comprehensive reconciliation. Each path runs the same amount verification to catch discrepancies."

---

## Failure Modes and Recovery

### Failure Matrix

| Failure Point | What Happens | Recovery |
|---------------|-------------|----------|
| App crashes after TX A, before Stripe call | Order + Payment exist, no Stripe session | Idempotent retry: detects missing `providerSessionId`, rebuilds line items from `Order.items`, creates Stripe session |
| App crashes after Stripe call, before TX B | Order + Payment exist, Stripe session exists but ID not saved locally | Idempotent retry: same as above. Stripe deduplicates via `idempotencyKey("checkout-session:" + orderId)` |
| Stripe API timeout | No session created | Client gets error, retries with same idempotency key → normal flow |
| Stripe API returns error | No session created | `StripeSessionCreationException` → 502 Bad Gateway to client |
| User abandons Stripe page | Session expires after 31 min | `checkout.session.expired` webhook → EXPIRED + PAYMENT_EXPIRED |
| Webhook arrives but business TX fails | Event marked RETRYABLE_FAILED | Stripe retries (got non-200). Next attempt re-acquires the event. |
| Webhook arrives but event marking fails | Event stuck in PROCESSING | Subsequent Stripe retry sees PROCESSING → skips. Needs manual cleanup or TTL-based reset. |
| Amount mismatch in webhook | RECONCILIATION_FAILED (terminal) | Requires human investigation. No auto-recovery. |
| Two webhooks for same order arrive simultaneously | Pessimistic lock serializes them | Second webhook waits, then checks terminal state → skips if already PAID |
| Webhook signature invalid | Rejected with 400 | No event recorded. Could be an attacker or misconfigured secret. |
| Database down during webhook | Exception propagates | Stripe gets 500, retries later |
| Stripe down during status polling | `Session.retrieve()` throws | Caught, logged as warning, status returned as-is from DB |

### The "Stuck PROCESSING" Edge Case

If the app crashes after inserting a webhook event (status=PROCESSING) but before processing it, the event is stuck. Subsequent Stripe retries see PROCESSING and skip it (assuming another instance is handling it).

**Current mitigation:** None — requires manual intervention.

**Production fix:** Add a TTL check: if an event has been PROCESSING for more than N minutes, treat it as RETRYABLE_FAILED and re-acquire it. This could be a scheduled job or a check in the `tryAcquire` logic.

---

## Security Architecture

### Authentication Model

The payment system uses two different authentication mechanisms:

| Endpoint | Auth Method | Why |
|----------|------------|-----|
| `POST /checkout` | JWT (Bearer token) | User-facing — standard auth |
| `GET /checkout/{orderId}/status` | JWT + owner check | User can only see their own orders |
| `POST /stripe/webhook` | Stripe signature (HMAC) | Stripe can't send JWT. Server-to-server auth via shared secret. |

### Webhook Signature Verification

```java
Event event = Webhook.constructEvent(payload, stripeSignature, webhookSecret);
```

Stripe signs every webhook with HMAC-SHA256 using the shared `STRIPE_WEBHOOK_SECRET`. The `Webhook.constructEvent()` method:
1. Extracts the timestamp and signature from the `Stripe-Signature` header
2. Computes `HMAC-SHA256(timestamp + "." + payload, webhookSecret)`
3. Compares with the provided signature
4. Rejects if the timestamp is too old (replay protection)

**Why not JWT for webhooks?** Stripe doesn't support JWT. The HMAC signature is actually stronger for this use case — it authenticates the entire payload (not just the sender), preventing tampering.

### Spring Security Configuration

```java
// SecurityConfig — ordering matters
.requestMatchers(STRIPE_WEBHOOK_URL).permitAll()  // Before payment auth rule
.requestMatchers(PAYMENT_URL).authenticated()       // After webhook rule
```

The webhook `permitAll()` rule must appear before the payment `authenticated()` rule. Spring Security evaluates rules in order — the first match wins.

### Rate Limiting

Payment endpoints have a dedicated rate limit bucket: 20 requests/minute per client IP. This applies to both authenticated endpoints and the webhook endpoint. The webhook endpoint is `permitAll()` in Spring Security, so the rate limiter is the only guard against abuse when Stripe is disabled.

### PCI Compliance

Iced Latte qualifies for **PCI DSS SAQ-A** (the simplest self-assessment):

- No card data entry on our domain (full redirect to Stripe)
- No card data transmission through our servers
- No card data storage anywhere in our application
- No Stripe.js loaded (no client-side tokenization)
- The only Stripe-related data we handle: session IDs, payment intent IDs, amounts, and webhook events

### The Feature Toggle

```java
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class PaymentEndpoint implements PaymentApi { ... }
```

When `stripe.enabled=false`, no payment beans are registered. The endpoints don't exist — they return 404, not 403. This is a clean feature toggle at the Spring container level, not a runtime if-check.

---

## Frontend Architecture — BFF Proxy and Polling

### The BFF (Backend for Frontend) Proxy

All API calls from the browser go through a Next.js catch-all route handler at `/api/proxy/[...path]/route.ts`. This is the **BFF proxy pattern**.

```
Browser → /api/proxy/payment/checkout → Next.js route handler → http://localhost:8083/api/v1/payment/checkout
```

**Why a proxy?**

1. **httpOnly cookie security.** JWT tokens are stored in httpOnly cookies — JavaScript cannot read them. The proxy reads the cookie and adds the `Authorization: Bearer` header before forwarding to the backend. This eliminates XSS-based token theft.

2. **Same-origin requests.** The browser calls `/api/proxy/*` (same origin) instead of `http://localhost:8083/*` (cross-origin). No CORS preflight needed.

3. **Input sanitization.** The proxy validates path segments against `ALLOWED_PATH_RE = /^[a-zA-Z0-9/_-]+$/` and query parameters against a strict regex. Path traversal attacks are blocked.

4. **Header forwarding.** The proxy selectively forwards `Idempotency-Key`, `X-Session-ID`, `X-Trace-ID`, and `X-Correlation-ID`. It doesn't blindly forward all headers.

### Client-Side Routing

The axios interceptor automatically routes requests based on environment:

```typescript
// shared/api/client.ts
if (typeof window === 'undefined') {
  config.url = `${NEXT_PUBLIC_API_URL}/${path}`  // SSR: direct to backend
} else {
  config.url = `/api/proxy/${path}`  // Browser: through proxy
}
```

Server-side rendering calls the backend directly (no proxy needed — no browser cookies to manage). Browser calls go through the proxy.

### Polling Strategy

The success page uses bounded polling with a fixed interval:

```typescript
// CheckoutSuccess.tsx
const MAX_RETRIES = 5
const POLL_INTERVAL_MS = 2000
```

**Tradeoffs:**
- Simple implementation — no WebSocket or SSE infrastructure
- Bounded — max 10 seconds of polling, then shows "pending" with a link to orders
- Abort-safe — `AbortController` cancels in-flight requests on component unmount
- No exponential backoff — fixed 2s interval (acceptable for 5 retries)

**Why not WebSockets?** The payment confirmation is a one-time event. Setting up a WebSocket connection for a single message is overkill. Polling 5 times over 10 seconds is simpler and sufficient.

### Cart State Management

The cart is **not** cleared when the user clicks "Place order." It's cleared only after the backend confirms payment:

```typescript
if (result.orderStatus === 'PAID') {
  resetCart()  // Only after confirmed payment
}
```

If the user closes the Stripe tab, the cart is still intact. The backend clears the cart in the webhook handler (atomically with the payment confirmation). The frontend clears its local state when polling confirms PAID.

---

## Database Design

### payments table

```sql
CREATE TABLE public.payments (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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
    created_at                  TIMESTAMPTZ  DEFAULT current_timestamp,
    updated_at                  TIMESTAMPTZ
);

-- Application-level idempotency
CREATE UNIQUE INDEX idx_payments_user_checkout_idempotency
    ON payments (user_id, checkout_idempotency_key)
    WHERE checkout_idempotency_key IS NOT NULL;

-- Sparse index for refund lookups
CREATE UNIQUE INDEX idx_payments_provider_payment_intent
    ON payments (provider_payment_intent_id)
    WHERE provider_payment_intent_id IS NOT NULL;
```

**Key design decisions:**

- **`amount_minor BIGINT`** — stores cents (e.g., $25.00 = 2500). Never use floating-point for money. `BIGINT` avoids rounding errors and matches Stripe's format.
- **`order_id UNIQUE`** — one payment per order. Enforced at DB level.
- **`provider_session_id UNIQUE`** — prevents duplicate Stripe sessions.
- **Partial unique index** on `(user_id, checkout_idempotency_key)` — only indexes non-null keys. Keeps the index small.
- **Sparse index** on `provider_payment_intent_id` — only indexes non-null values. Used for refund lookups (`charge.refunded` webhook).
- **`raw_event_id` + `latest_event_type`** — audit trail of the last webhook event that modified this payment. Not full event sourcing, but enough for debugging.

### stripe_webhook_events table

```sql
CREATE TABLE public.stripe_webhook_events (
    stripe_event_id  VARCHAR(255) PRIMARY KEY,  -- Natural key from Stripe
    event_type       VARCHAR(100) NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'PROCESSING',
    received_at      TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    processed_at     TIMESTAMPTZ,
    failure_reason   TEXT
);
```

**Natural key:** Stripe event IDs are globally unique. Using them as the PK means the INSERT itself is the deduplication check — no separate SELECT needed.

### Why Not Event Sourcing?

The `payments` table stores current state, not a log of all events. Full event sourcing would store every webhook event and derive the current state by replaying them. We chose current-state storage because:

- Simpler queries (no event replay needed)
- The `stripe_webhook_events` table provides an audit trail of processed events
- `raw_event_id` + `latest_event_type` on the payment record tracks the last event
- For a full audit trail, Stripe's Dashboard already stores all events

---

## Backend Package Structure

```
payment/
├── endpoint/
│   └── PaymentEndpoint.java          — Thin REST controller (3 endpoints)
├── api/
│   ├── CheckoutPaymentService           — Saga orchestrator (non-TX)
│   ├── CheckoutPaymentTransactionService — TX A + TX B for checkout
│   ├── CheckoutPreparation              — Value object between saga stages
│   ├── StripeCheckoutSessionCreator     — Anti-corruption layer for Stripe SDK
│   ├── StripeSessionResult              — Value object (sessionId + URL)
│   ├── StripeWebhookService             — Webhook pipeline coordinator (non-TX)
│   ├── StripeWebhookBusinessProcessor   — Webhook business logic (TX)
│   ├── StripeWebhookEventRecorder       — Event dedup coordinator (non-TX)
│   ├── StripeWebhookEventTransactionService — Event dedup (REQUIRES_NEW TX)
│   └── PaymentStatusService             — Polling + reconciliation fallback
├── entity/
│   ├── Payment.java                     — JPA entity
│   ├── PaymentStatus.java              — Enum with isTerminal()
│   ├── PaymentProvider.java             — Enum (STRIPE only)
│   ├── StripeWebhookEvent.java          — Natural key entity for dedup
│   └── WebhookEventStatus.java          — Enum (PROCESSING/PROCESSED/RETRYABLE_FAILED)
├── repository/
│   ├── PaymentRepository.java           — Pessimistic locking queries
│   └── StripeWebhookEventRepository.java
├── converter/
│   └── StripeSessionLineItemListConverter.java — MapStruct cart→Stripe
└── exception/
    ├── StripeSessionCreationException.java
    ├── PaymentEventProcessingException.java
    └── handler/
        └── PaymentExceptionHandler.java — @RestControllerAdvice
```

**Why so many classes?** Each class exists for a specific reason — usually a transaction boundary requirement or a separation of concerns. If you see two classes that look like they could be one, check if one has `@Transactional`. If yes, the split exists because Spring requires it.

---

## Design Patterns Catalogue

Every pattern used in the payment integration, with the specific class that implements it.

### Saga Pattern (Orchestrated)
**Where:** `CheckoutPaymentService`
**What:** Coordinates TX A → Stripe API → TX B as a sequence of local transactions with compensating actions (idempotent retry for crash recovery, webhook-driven compensation for payment failures).

### Anti-Corruption Layer
**Where:** `StripeCheckoutSessionCreator`
**What:** Translates domain concepts (Order, cart items) into Stripe SDK parameters. Isolates the rest of the codebase from Stripe's API surface. If Stripe changes their API, only this class changes.

### Insert-First Deduplication
**Where:** `StripeWebhookEventTransactionService`
**What:** Attempts INSERT with the event ID as PK. Constraint violation = duplicate. Simpler and more race-safe than SELECT-then-INSERT (which has a TOCTOU gap).

### State Machine with Guards
**Where:** `PaymentStatus.isTerminal()`, `OrderStatusTransitioner.TRANSITIONS`
**What:** Explicit enumeration of valid state transitions. Terminal states are immutable. Guards enforce business rules (cancellation deadline, owner-only refund).

### Transaction Script with Separated Bean
**Where:** `CheckoutPaymentTransactionService`, `StripeWebhookEventTransactionService`
**What:** Transactional methods extracted into separate beans to avoid Spring's self-invocation trap. The coordinator bean is non-transactional and calls the TX bean through the Spring proxy.

### Programmatic Transaction (TransactionTemplate)
**Where:** `PaymentStatusService.syncPaidStatus()`
**What:** Alternative to the separated-bean pattern. Uses `TransactionTemplate` for a single method that needs a transaction within a non-transactional class.

### Feature Toggle (Container-Level)
**Where:** `@ConditionalOnProperty(name = "stripe.enabled")` on `PaymentEndpoint`, `CheckoutPaymentService`, `StripeCheckoutSessionCreator`, `StripeWebhookService`
**What:** When disabled, beans aren't registered. Endpoints don't exist (404, not 403). No runtime if-checks.

### OpenAPI-First (Contract-First API)
**Where:** `payment-openapi.yaml` → generated `PaymentApi` interface → `PaymentEndpoint implements PaymentApi`
**What:** The API contract is defined in OpenAPI YAML. Maven generates Java interfaces and DTOs. The controller implements the generated interface. If the spec changes, the code won't compile until the implementation matches.

### BFF Proxy
**Where:** `/api/proxy/[...path]/route.ts` (frontend)
**What:** Next.js route handler acts as a reverse proxy. Adds auth headers from httpOnly cookies, sanitizes input, forwards to backend. Eliminates CORS and protects tokens from XSS.

### Polling with Bounded Retries
**Where:** `CheckoutSuccess.tsx`
**What:** Polls backend every 2s, max 5 times. Graceful degradation — shows "pending" if retries exhausted. Simpler than WebSockets for a one-time event.

### Snapshot Semantics
**Where:** `OrderCreator.createPendingPaymentOrder()`
**What:** Cart items are copied into `OrderItem` entities at checkout time. The order is a frozen snapshot — immune to subsequent cart modifications or price changes.

### MapStruct Converter
**Where:** `StripeSessionLineItemListConverter`
**What:** Compile-time code generation for mapping `ShoppingCartItemDto` → Stripe `SessionCreateParams.LineItem`. Includes custom `@Named` method for dollar-to-cents conversion.

### Domain Events
**Where:** `OrderStatusTransitioner` publishes `OrderStatusChangedEvent`
**What:** Spring `ApplicationEventPublisher` for loose coupling. Other modules react to order changes without the order module knowing about them.

### Exception Translation Layer
**Where:** `PaymentExceptionHandler` (`@RestControllerAdvice`)
**What:** Maps domain exceptions to HTTP status codes. `StripeSessionCreationException` → 502 (upstream failure). `PaymentEventProcessingException` → 400 (invalid webhook).

---

## Scaling Considerations

### What Works at Scale (No Changes Needed)

- **Pessimistic locks** work across multiple application instances (they're database-level locks, not in-memory)
- **Event deduplication** works across instances (database PK constraint)
- **Idempotency** works across instances (database unique index)
- **Stateless endpoints** — no server-side session state, horizontal scaling is straightforward

### What Would Change at Scale

| Current Design | Production at Scale | Why |
|---------------|-------------------|-----|
| Synchronous webhook processing | Async queue (SQS/Kafka) | Decouple webhook receipt from processing. Return 200 immediately, process later. Handles Stripe's retry timeout. |
| No outbound rate limiting | Token bucket / circuit breaker for Stripe API | Stripe has rate limits (100 req/s in test, 10,000 in live). Need backpressure. |
| Polling fallback only on user request | Scheduled reconciliation job | Background job scans non-terminal payments older than N minutes. Catches cases where no user polls. |
| `Stripe.apiKey` global static | Per-request `RequestOptions` | Global mutable static doesn't work for multi-tenant or parallel testing. |
| Single `payments` table | Partitioned by `created_at` | Time-based partitioning for archival and query performance on large datasets. |
| `rawEventId` + `latestEventType` | Full event sourcing | Store all webhook events per payment for complete audit trail and replay capability. |
| Cart deletion in webhook TX | Async cart cleanup | Decouples cart lifecycle from payment confirmation. Reduces webhook TX duration. |
| No monitoring | Micrometer counters/timers | Payment success/failure rates, webhook processing latency, reconciliation mismatch alerts. |

### Horizontal Scaling Gotcha

With multiple instances, two instances might receive the same webhook event simultaneously (Stripe doesn't guarantee single delivery). The insert-first dedup handles this — only one INSERT succeeds, the other gets a constraint violation. The pessimistic lock on the Payment row handles different events for the same order arriving at different instances.

---

## Interview Cheat Sheet

Quick answers for common system design interview questions about payment integration.

### "How would you design a payment system?"

1. **Backend owns the order lifecycle.** Create order + payment record locally before calling the payment provider. The provider is just the payment rail.
2. **Use Hosted Checkout** for MVP. Redirect to the provider's page. Minimizes PCI scope.
3. **Webhooks are the source of truth** for payment status. The redirect back to your site is unreliable.
4. **Idempotency at every layer.** Client key → app check → DB constraint → provider key.
5. **Never hold a DB transaction during a network call.** Split into TX A → API call → TX B.
6. **Reconciliation fallback.** Never rely on a single delivery mechanism for money. Have a polling fallback and a scheduled reconciliation job.

### "How do you handle duplicate payments?"

Seven layers of idempotency: client-generated key, application-level check, database unique index, Stripe-level idempotency key, webhook event dedup table, terminal state guard, pessimistic lock on payment row. Each catches a different class of failure.

### "What happens if the webhook doesn't arrive?"

Three fallback mechanisms: (1) Stripe retries for up to 3 days, (2) the success page polling endpoint calls `Session.retrieve()` directly as a sync fallback, (3) a scheduled reconciliation job (production) scans stale payments.

### "How do you prevent race conditions?"

Pessimistic locking (`SELECT FOR UPDATE`) on the Payment row serializes concurrent webhook processing. Event-level dedup prevents duplicate processing of the same event. Terminal state guards prevent re-processing of completed payments.

### "Why not use a distributed transaction?"

Two-phase commit (2PC/XA) across PostgreSQL and Stripe is not possible — Stripe doesn't support XA. Even if it did, 2PC is slow, fragile, and doesn't scale. Instead, we use a saga pattern with idempotent retry and compensating actions.

### "How do you ensure exactly-once processing?"

Stripe delivers at-least-once. We convert to effectively-once through: (1) event dedup table with insert-first pattern, (2) pessimistic lock on payment row, (3) terminal state guard. The combination ensures each payment state change happens exactly once.

### "What's your consistency model?"

Eventually consistent between local DB and Stripe. During the consistency window (between Stripe payment and webhook arrival), our DB is stale. Three reconciliation mechanisms close the gap: webhook (primary), polling fallback (on-demand), scheduled job (comprehensive).

### "How do you handle partial failures in the checkout flow?"

The checkout is a 3-stage saga: TX A → Stripe API → TX B. If the app crashes between stages, the idempotent retry path detects the incomplete state (missing `providerSessionId`) and resumes from where it left off. Stripe's own idempotency key prevents duplicate sessions.

---

## Configuration Reference

### Backend (.env)

```bash
# Stripe (TEST MODE ONLY — no real money is charged)
STRIPE_ENABLED=false
STRIPE_SECRET_KEY=sk_test_...your_key...
STRIPE_WEBHOOK_SECRET=whsec_...your_key...

# Frontend URL (for Stripe success/cancel redirect URLs)
FRONTEND_URL=http://localhost:3000
```

- `STRIPE_ENABLED=false` → no payment beans registered, endpoints don't exist
- Contributors should keep `STRIPE_ENABLED=false` unless they are explicitly testing checkout with Stripe test keys.
- Production sets `STRIPE_ENABLED=true` through Vault-managed runtime env.
- `STRIPE_SECRET_KEY` → server-side secret from Stripe Dashboard → API keys
- `STRIPE_WEBHOOK_SECRET` → signing secret for webhook signature verification
- Never use `sk_live_...` keys. Only `sk_test_...`.

### Frontend (.env.local)

```bash
NEXT_PUBLIC_STRIPE_ENABLED=false
NEXT_PUBLIC_API_URL=http://localhost:8083/api/v1
NEXT_PUBLIC_FRONTEND_URL=http://localhost:3000
```

- `NEXT_PUBLIC_STRIPE_ENABLED=true` → enables checkout button. Missing or `false` → disabled.
- Contributors should keep `NEXT_PUBLIC_STRIPE_ENABLED=false` unless their local backend has Stripe enabled.
- Production sets `NEXT_PUBLIC_STRIPE_ENABLED=true` through Vault-managed build env.
- No Stripe publishable key needed — the frontend never loads Stripe.js.

---

## Local Development Setup

### Step 1 — Get Stripe Test Keys

1. Go to https://dashboard.stripe.com/test/apikeys
2. Copy the **Secret key** (`sk_test_...`)
3. Paste into `Iced-Latte/.env` as `STRIPE_SECRET_KEY`
4. Set `STRIPE_ENABLED=true`

### Step 2 — Webhook Secret (Two Options)

**Option A — Without Stripe CLI (simpler):**
Set `STRIPE_WEBHOOK_SECRET` to any placeholder. The webhook endpoint will reject events, but the **sync fallback** in `PaymentStatusService` calls Stripe directly when the success page polls. This is enough for local testing.

**Option B — With Stripe CLI (full webhook flow):**
```bash
brew install stripe/stripe-cli/stripe
stripe login
stripe listen --forward-to localhost:8083/api/v1/payment/stripe/webhook
# Copy the whsec_... value it prints → paste into .env
```

### Step 3 — Start Everything

```bash
# Terminal 1: Backend
cd Iced-Latte && export $(cat .env | xargs) && mvn spring-boot:run

# Terminal 2: Frontend
cd Iced-Latte-Frontend && npm run dev

# Terminal 3 (optional): Stripe CLI
stripe listen --forward-to localhost:8083/api/v1/payment/stripe/webhook
```

### Step 4 — Test

1. Open http://localhost:3000, sign in
2. Add items to cart → checkout
3. Fill recipient info, click "Place order"
4. On Stripe's page: card `4242 4242 4242 4242`, any future expiry, any CVC
5. Click Pay → redirected to success page → ✅ Payment confirmed

---

## Test Cards

| Card Number | Behavior |
|-------------|----------|
| `4242 4242 4242 4242` | Succeeds immediately |
| `4000 0000 0000 3220` | Requires 3D Secure |
| `4000 0000 0000 0002` | Declined |
| `4000 0000 0000 9995` | Insufficient funds |
| `4000 0025 0000 3155` | Requires auth, then succeeds |

Full list: https://docs.stripe.com/testing#cards

---

## Glossary

| Term | Meaning |
|------|---------|
| **Saga** | A sequence of local transactions with compensating actions, replacing distributed transactions in distributed systems. |
| **Idempotency** | The property that doing the same operation twice produces the same result. Critical for payment systems to prevent double charges. |
| **Eventual consistency** | A consistency model where all nodes eventually converge to the same state, but may be temporarily inconsistent. |
| **Pessimistic locking** | Acquiring a database lock before reading/writing, blocking other transactions. Used when write contention is expected. |
| **Optimistic locking** | Reading without locks, checking for conflicts on write via a version counter. Used when contention is rare. |
| **REQUIRES_NEW** | Spring transaction propagation that creates a new independent transaction, suspending any existing one. |
| **Self-invocation trap** | Spring gotcha where calling a `@Transactional` method from within the same class bypasses the proxy. |
| **Terminal state** | A state that cannot be changed by subsequent events (PAID, FAILED, EXPIRED, REFUNDED, RECONCILIATION_FAILED). |
| **Reconciliation** | Verifying that two systems agree on the same data. Here: checking Stripe's amount matches our local record. |
| **Anti-corruption layer** | A translation layer that isolates your domain from an external system's API surface. |
| **Insert-first dedup** | Attempting INSERT with a unique constraint; catching the violation means duplicate. Race-safe alternative to SELECT-then-INSERT. |
| **BFF (Backend for Frontend)** | A server-side proxy that sits between the browser and the backend API, handling auth, CORS, and input sanitization. |
| **SAQ-A** | The simplest PCI DSS self-assessment questionnaire, applicable when card data never touches your systems. |
| **Snapshot semantics** | Copying data at a point in time so subsequent changes don't affect the copy. Cart items are snapshot into order items. |
| **Hosted Checkout** | Stripe's hosted payment page. The user is redirected there — your app never sees card details. |
| **Webhook** | An HTTP POST that Stripe sends to your server when something happens (payment completed, expired, refunded). |
| **Payment Intent** | A Stripe object representing the actual charge. Created automatically by the Checkout Session. |
| **Sync fallback** | When the webhook hasn't arrived, calling Stripe's API directly to check payment status. The safety net. |
| **Feature toggle** | `@ConditionalOnProperty` — disables entire Spring beans at container startup. No runtime overhead. |
| **Domain event** | An event published within the application (e.g., `OrderStatusChangedEvent`) for loose coupling between modules. |

---

*This guide was written for Iced Latte v4.0.5 (May 2026). Stripe API version: 2025-04-30.basset.*

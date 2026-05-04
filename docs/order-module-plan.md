# Order Module — Comprehensive Implementation Plan

> **Project:** Iced Latte · **Module:** `com.zufar.icedlatte.order`
> **Date:** 2026-05-03 · **Status:** Draft

---

## 1. Current State Analysis

### What exists today

| Layer | Status | Details |
|-------|--------|---------|
| Endpoints | 2 of ~9 needed | `GET /api/v1/orders` (list, no pagination), `POST /api/v1/orders` (create) |
| Entity model | Minimal | `Order` + `OrderItem` tables, 4-status enum (CREATED, PAID, DELIVERY, FINISHED) |
| Business logic | MVP only | Create order from cart, list orders with optional status filter |
| Stripe integration | Partial | Webhook creates order on `checkout.session.completed`, idempotency via `(userId, sessionId)` |
| Tests | 10 unit tests | No integration tests, no controller tests, Stripe webhook path untested |
| DB indexes | Missing | No index on `orders.user_id` or `order_item.order_id` |
| Frontend | Basic | Order history with status filter tabs, checkout form with address picker, no pagination, no Stripe integration, no cancel/refund UI. See [Section 9](#9-frontend-impact-analysis) for full analysis. |

### Known bugs and inconsistencies

1. **Duplicate `createdAt` field** — `Order` entity declares its own `@CreationTimestamp createdAt` AND inherits one from `AuditableEntity` with `@CreatedDate`. Potential mapping conflict.
2. **Cart not cleared on REST order creation** — `POST /api/v1/orders` creates an order but leaves the cart intact. The Stripe webhook path clears it. Inconsistent behavior.
3. **Stripe path doesn't set recipient info** — `createOrderAndDeleteCart()` never sets `recipientName`/`recipientSurname`, risking NOT NULL constraint violations (saved by `DEFAULT ''` in DB).
4. **Different address sourcing** — REST path uses address from request body; Stripe path uses `user.getAddress()` (profile address).
5. **SERIALIZABLE on read-only method** — `OrderProvider.getOrderEntityByUserAndSession()` uses SERIALIZABLE isolation for a simple lookup.
6. **No FK on `orders.user_id`** — no referential integrity to `user_details` table.
7. **`OrderItemRepository` unused** — items are cascaded through `Order` entity; the repository has no custom methods.
8. **License mismatch** — `order-openapi.yaml` says MIT; project now uses a custom repository license.
9. **`OrderEndpoint` doesn't implement generated API interface** — every other endpoint in the project (CartEndpoint, ProductsEndpoint, UserEndpoint, PaymentEndpoint, etc.) implements its generated OpenAPI interface (e.g., `implements ShoppingCartApi`). OrderEndpoint does not, which means it bypasses the OpenAPI-generated validation and contract enforcement.
10. **Stripe payment flow is dormant** — The backend has a full Stripe Checkout flow (`POST /api/v1/payment` creates a session, webhook creates order on completion), but the frontend never calls it. The frontend's checkout flow calls `POST /api/v1/orders` directly without any payment step. The frontend's `/orders/success` page (which handles Stripe redirect) exists but is unreachable from the current checkout. This means there are two parallel order creation paths — one active (REST, no payment) and one dormant (Stripe webhook) — with different behaviors (see bugs #2, #3, #4).
11. **Frontend clears cart client-side only** — `useCheckoutForm` calls `resetCart()` (Zustand store + localStorage) on order creation success, but the backend `POST /orders` does NOT clear the server-side cart (bug #2). If the user logs in on another device, the old cart items are still there.

---

## 2. Target Architecture

### New order status lifecycle

```
CREATED ──→ PAID ──→ SHIPPED ──→ DELIVERED
  │           │
  │           └──→ REFUND_REQUESTED ──→ REFUNDED
  │
  └──→ CANCELLED
```

**Statuses:** CREATED, PAID, SHIPPED, DELIVERED, CANCELLED, REFUND_REQUESTED, REFUNDED

**Transition rules:**

| From | Event | To | Guard |
|------|-------|----|-------|
| CREATED | PAYMENT_CONFIRMED | PAID | Stripe webhook confirms payment |
| CREATED | CANCEL | CANCELLED | Within cancellation window (configurable, default 30 min) |
| PAID | SHIP | SHIPPED | Admin-only action |
| PAID | CANCEL | CANCELLED | Within cancellation window |
| PAID | REQUEST_REFUND | REFUND_REQUESTED | User-initiated |
| SHIPPED | DELIVER | DELIVERED | Admin-only action |
| REFUND_REQUESTED | REFUND_CONFIRMED | REFUNDED | Stripe webhook confirms refund |

### New entity model

```
orders (enhanced)
├── id (UUID PK)
├── user_id (UUID FK → user_details)
├── session_id (VARCHAR)
├── status (VARCHAR — expanded enum)
├── version (INT — optimistic locking)
├── idempotency_key (VARCHAR UNIQUE — prevents duplicate orders)
├── address_id (UUID FK → address — snapshot of delivery address at order time)
├── recipient_name, recipient_surname, recipient_phone
├── items_quantity, items_total_price
├── cancellation_deadline (TIMESTAMPTZ — when cancel window expires)
├── stripe_payment_intent_id (VARCHAR — for refunds)
├── refund_reason (VARCHAR), refunded_at (TIMESTAMPTZ)
├── created_at, updated_at, created_by, updated_by (audit)
└── items → order_item (existing, add index)

order_status_history (NEW)
├── id (UUID PK)
├── order_id (UUID FK → orders)
├── old_status (VARCHAR)
├── new_status (VARCHAR)
├── changed_by (UUID — user or system)
├── reason (VARCHAR — optional, e.g. "Customer requested refund")
└── changed_at (TIMESTAMPTZ)
```

### New endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/orders` | Paginated order history with filters | User |
| `GET` | `/api/v1/orders/{orderId}` | Single order detail | User (own orders only) |
| `POST` | `/api/v1/orders` | Create order from cart | User |
| `POST` | `/api/v1/orders/{orderId}/cancel` | Cancel order | User (within window) |
| `POST` | `/api/v1/orders/{orderId}/refund` | Request refund | User |
| `POST` | `/api/v1/orders/{orderId}/reorder` | Re-order / buy again | User |
| `GET` | `/api/v1/orders/{orderId}/history` | Order status change history | User |
| `PATCH` | `/api/v1/admin/orders/{orderId}/status` | Update order status | Admin |
| `GET` | `/api/v1/admin/orders` | List all orders (admin view) | Admin |

**Note:** Phase 5 also introduces 4 payment method endpoints (`/api/v1/payment/setup-intent`, `/api/v1/payment/methods`, etc.) — these live in the payment module, not the order module. See Phase 5 for details.

---

## 3. Implementation Phases

### Phase 1 — Fix Existing Bugs & Schema Foundation

**Goal:** Clean up the current code, fix bugs, add missing indexes, and prepare the schema for the full lifecycle.

**Database migrations:**

1. Add index on `orders.user_id`:
   ```sql
   CREATE INDEX idx_orders_user_id ON public.orders (user_id);
   ```
2. Add index on `order_item.order_id`:
   ```sql
   CREATE INDEX idx_order_item_order_id ON public.order_item (order_id);
   ```
3. Add FK constraint on `orders.user_id`:
   ```sql
   ALTER TABLE public.orders
       ADD CONSTRAINT fk_orders_user
       FOREIGN KEY (user_id) REFERENCES user_details(id);
   ```
4. Add `version` column for optimistic locking:
   ```sql
   ALTER TABLE public.orders ADD COLUMN version INT NOT NULL DEFAULT 0;
   ```
5. Add `idempotency_key` column:
   ```sql
   ALTER TABLE public.orders ADD COLUMN idempotency_key VARCHAR(64);
   CREATE UNIQUE INDEX idx_orders_idempotency_key ON public.orders (idempotency_key) WHERE idempotency_key IS NOT NULL;
   ```
6. Add `cancellation_deadline` column:
   ```sql
   ALTER TABLE public.orders ADD COLUMN cancellation_deadline TIMESTAMPTZ;
   ```
7. Create `order_status_history` table (see [database migration reference](order-module-db-migrations.md)).
8. Migrate `address_id` FK from `address` to `delivery_address` table (orders should reference the user's delivery address book, not the legacy profile address). **Note:** This is a complex migration — existing orders reference the `address` table. The migration must: (a) create snapshot copies in `address` for existing orders (preserving data), (b) update the FK constraint. See [database migration reference](order-module-db-migrations.md) for the full script. This migration can be deferred to Phase 4 if needed, since Phase 4 is where the delivery address book integration happens.

**Code fixes:**

- Remove duplicate `createdAt` field from `Order.java` — rely on `AuditableEntity.createdAt` only
- Add `@Version` field to `Order.java` for optimistic locking
- Add `idempotencyKey` and `cancellationDeadline` fields to `Order.java`
- Fix `OrderCreator.create()` to clear the cart after order creation (match Stripe webhook behavior)
- Fix `OrderCreator.createOrderAndDeleteCart()` to set `recipientName`/`recipientSurname` from user profile
- Change `OrderProvider.getOrderEntityByUserAndSession()` isolation from SERIALIZABLE to READ_COMMITTED
- Remove unused `OrderItemRepository` (or add custom queries if needed later)
- Fix license in `order-openapi.yaml` from MIT to the repository's current license notice
- Delete duplicated `maskSessionId()` utility — extract to a shared helper
- Make `OrderEndpoint` implement the generated OpenAPI interface (e.g., `implements OrdersApi`) — align with all other endpoints in the project

**Tests:**
- Unit tests for all bug fixes
- Integration test for order creation (both REST and Stripe webhook paths)

**Estimated effort:** 3–4 days

---

### Phase 2 — Order State Machine & Status Transitions

**Goal:** Implement the order lifecycle with explicit transition rules, guards, domain events, and audit trail.

**Why this matters:** State machine design is a core backend pattern. Every workflow system (CI/CD pipelines, approval flows, document processing) uses the same concepts. Building it by hand teaches you the pattern without framework abstraction.

**New classes:**

```
order/
├── api/
│   ├── OrderStateMachine.java          # Transition table + guard logic
│   ├── OrderStatusTransitioner.java    # Service: validates + executes transitions
│   └── OrderStatusHistoryRecorder.java # Logs every transition to order_status_history
├── entity/
│   └── OrderStatusHistory.java         # JPA entity for audit trail
├── repository/
│   └── OrderStatusHistoryRepository.java
├── event/
│   └── OrderStatusChangedEvent.java    # Spring ApplicationEvent
├── exception/
│   ├── InvalidOrderStateTransitionException.java
│   └── OrderCancellationWindowExpiredException.java
└── validator/
    └── OrderStatusTransitionValidator.java
```

**State machine implementation (no Spring State Machine — plain Java):**

```java
public class OrderStateMachine {

    private static final Map<OrderStatus, Map<OrderEvent, OrderStatus>> TRANSITIONS = Map.of(
        CREATED,           Map.of(PAYMENT_CONFIRMED, PAID, CANCEL, CANCELLED),
        PAID,              Map.of(SHIP, SHIPPED, CANCEL, CANCELLED, REQUEST_REFUND, REFUND_REQUESTED),
        SHIPPED,           Map.of(DELIVER, DELIVERED),
        REFUND_REQUESTED,  Map.of(REFUND_CONFIRMED, REFUNDED)
    );

    public OrderStatus transition(OrderStatus current, OrderEvent event) {
        return Optional.ofNullable(TRANSITIONS.get(current))
                .map(events -> events.get(event))
                .orElseThrow(() -> new InvalidOrderStateTransitionException(current, event));
    }
}
```

**Guard conditions (in `OrderStatusTransitionValidator`):**

| Transition | Guard |
|-----------|-------|
| `* → CANCELLED` | `OffsetDateTime.now().isBefore(order.getCancellationDeadline())` |
| `PAID → SHIPPED` | Caller has ADMIN role |
| `SHIPPED → DELIVERED` | Caller has ADMIN role |
| `PAID → REFUND_REQUESTED` | Order owner only |

**Domain events:**

- `OrderStatusChangedEvent(orderId, oldStatus, newStatus, changedBy, timestamp)` published via `ApplicationEventPublisher` after every successful transition
- `@EventListener` in `OrderStatusHistoryRecorder` persists the event to `order_status_history`
- Future notification module can subscribe to the same event with zero changes to order code (open/closed principle)

**Optimistic locking:**

- `@Version private Integer version;` on `Order` entity
- Prevents concurrent status updates (e.g., webhook and user both trying to transition simultaneously)
- On `OptimisticLockException`, Spring Retry (`@Retryable`) retries the transition — same pattern already used in the cart module

**Stripe webhook integration update:**

- `StripeWebhookService.handleCompleted()` now calls `OrderStatusTransitioner.transition(orderId, PAYMENT_CONFIRMED)` instead of creating the order directly
- Order creation happens at checkout initiation (`POST /api/v1/orders`), payment confirmation is a separate status transition
- This separates concerns: order creation ≠ payment confirmation

**Important flow change:** The current flow is: cart → `POST /api/v1/payment` (creates Stripe session from cart) → Stripe checkout → webhook creates order + deletes cart. The new flow must be: cart → `POST /api/v1/orders` (creates order from cart, clears cart) → `POST /api/v1/payment` (creates Stripe session from the **order**, not the cart) → Stripe checkout → webhook transitions order to PAID. This means `StripeSessionCreator` must be updated to build Stripe line items from the Order entity instead of the ShoppingCart. The order ID should be stored in the Stripe session metadata alongside the userId.

**Tests:**
- Unit tests for `OrderStateMachine` — test every valid transition and every invalid transition
- Unit tests for guard conditions (cancellation window, role checks)
- Unit test for `OrderStatusHistoryRecorder` event listener
- Integration test: full lifecycle CREATED → PAID → SHIPPED → DELIVERED
- Concurrency test: two simultaneous transitions on the same order — one succeeds, one gets `OptimisticLockException`

**Estimated effort:** 4–5 days

---

### Phase 3 — Paginated Order History with Filtering & Sorting

**Goal:** Build an Amazon-style order history: paginated, filterable by year/status/date range, sortable by date/price.

**Why this matters:** Dynamic query building with JPA Specifications is the standard approach for filter-heavy UIs. Offset-based pagination is the pragmatic choice here — it integrates directly with Spring Data's `Pageable` and `JpaSpecificationExecutor`, which the project already uses for products and reviews.

**New/modified classes:**

```
order/
├── api/
│   ├── OrdersProvider.java             # REWRITE — add pagination + Specification-based filtering
│   └── OrderDetailProvider.java        # NEW — get single order by ID with authorization check
├── converter/
│   └── OrderDtoConverter.java          # UPDATE — add summary DTO mapping
└── validator/
    └── OrderFilterValidator.java       # NEW — validate filter parameters
```

**Query parameters for `GET /api/v1/orders`:**

| Parameter | Type | Description | Example |
|-----------|------|-------------|---------|
| `page` | int | Page number (0-based) | `0` |
| `size` | int | Page size (default 10, max 50) | `10` |
| `sortBy` | string | Sort field | `createdAt`, `itemsTotalPrice` |
| `sortDirection` | string | `ASC` or `DESC` (default `DESC`) | `DESC` |
| `status` | string[] | Filter by status(es) | `PAID,SHIPPED` |
| `year` | int | Filter by year | `2026` |
| `dateFrom` | date | Start of date range | `2026-01-01` |
| `dateTo` | date | End of date range | `2026-12-31` |

**JPA Specification approach:**

```java
public class OrderSpecifications {

    public static Specification<Order> belongsToUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Order> hasStatusIn(List<OrderStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Order> createdInYear(int year) {
        return (root, query, cb) -> cb.equal(
            cb.function("date_part", Integer.class, cb.literal("year"), root.get("createdAt")),
            year
        );
    }

    public static Specification<Order> createdBetween(OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) -> cb.between(root.get("createdAt"), from, to);
    }
}
```

**Response structure — paginated:**

```json
{
  "content": [
    {
      "id": "...",
      "status": "PAID",
      "createdAt": "2026-05-01T10:00:00Z",
      "itemsQuantity": 3,
      "itemsTotalPrice": 45.99,
      "firstItemName": "Nitro Cold Brew",
      "firstItemImageUrl": "https://..."
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 47,
  "totalPages": 5
}
```

**`GET /api/v1/orders/{orderId}` — single order detail:**

- Returns full order with items, address, recipient info, status history
- Authorization: only the order owner can access (compare `order.userId` with authenticated user)
- Throws `OrderNotFoundException` or `OrderAccessDeniedException`

**Repository changes:**

- `OrderRepository` extends `JpaSpecificationExecutor<Order>` in addition to `JpaRepository`
- Remove the existing `findAllByUserId` and `findAllByUserIdAndStatusIn` methods — replaced by Specifications
- Use `PageRequestFactory` from `common/` for consistent pagination (already exists in the project)

**PaginationConfig integration:**

- Add an `Orders` inner class to the existing `PaginationConfig` (in `common/config/`) — same pattern used for `Products` and `Reviews`:
  ```java
  @Getter @Setter
  public static class Orders {
      private int defaultPageSize = 10;
      private int maxPageSize = 50;
      private String defaultSortAttribute = "createdAt";
      private String defaultSortDirection = "desc";
  }
  ```
- Add corresponding properties to `application.yml` under `pagination.orders.*`
- Use `PaginationParametersValidator` from `common/` for input validation

**Tests:**
- Unit tests for each Specification (test SQL predicate generation)
- Integration test: create 15 orders across 2 years, verify year filter, status filter, date range, pagination, sorting
- Integration test: `GET /api/v1/orders/{orderId}` — own order returns 200, other user's order returns 403, nonexistent returns 404

**Estimated effort:** 3–4 days

---

### Phase 4 — Delivery Address Book Integration

**Goal:** Allow users to select from their saved delivery addresses when placing an order, instead of providing a full address every time.

**What already exists:**

- `DeliveryAddressEntity` — table `delivery_address` with fields: `id`, `user_id`, `label`, `line`, `city`, `country`, `postcode`, `is_default`
- Full CRUD endpoints already exist at `/api/v1/users/addresses` (create, list, update, delete, set default)
- Unique partial index ensures at most one default address per user

**Changes to order creation:**

The `CreateNewOrderRequestDto` will accept **either** a `deliveryAddressId` (reference to saved address) **or** an inline `address` object (for one-time addresses). At least one must be provided.

```json
// Option A: use saved address
{
  "deliveryAddressId": "550e8400-e29b-41d4-a716-446655440000",
  "recipientName": "John",
  "recipientSurname": "Doe"
}

// Option B: inline address (current behavior)
{
  "address": { "line": "123 Main St", "city": "London", "country": "UK", "postcode": "SW1A 1AA" },
  "recipientName": "John",
  "recipientSurname": "Doe"
}
```

**Implementation:**

- `OrderCreator.create()` — if `deliveryAddressId` is provided, fetch the `DeliveryAddressEntity`, verify it belongs to the authenticated user, and snapshot it into the order's address. If inline `address` is provided, use it directly (current behavior).
- **Snapshot, not reference** — the order stores a copy of the address at the time of order creation. If the user later updates their saved address, existing orders are unaffected. This is standard e-commerce behavior.
- Migrate the order's `address_id` FK from the legacy `address` table to a new `order_delivery_address` table (or keep using the existing `address` table as a snapshot store — decision depends on whether we want to decouple from the user profile address entity).

**Validation:**

- `OrderAddressValidator` — ensures exactly one of `deliveryAddressId` or `address` is provided
- If `deliveryAddressId` is provided, verify it exists and belongs to the user
- If inline `address` is provided, validate required fields (line, city, country, postcode)

**Stripe webhook path update:**

- `createOrderAndDeleteCart()` should use the user's default delivery address from the address book (instead of the legacy `user.getAddress()`)
- If no default address exists, fall back to the first address in the book, or fail with a clear error

**Tests:**
- Unit test: order creation with saved address ID
- Unit test: order creation with inline address
- Unit test: validation rejects request with neither address option
- Unit test: validation rejects request with both address options
- Integration test: create order using saved delivery address, verify address snapshot is independent

**Estimated effort:** 2–3 days

---

### Phase 5 — Stripe Saved Payment Methods & Selection

**Goal:** Let users save payment cards via Stripe and choose one at checkout, instead of entering card details every time.

**Why this matters:** PCI compliance awareness — you never store card numbers. Stripe handles all sensitive data; you only store Stripe's `PaymentMethod` IDs and last-4 digits for display.

**How Stripe saved cards work:**

1. **Stripe Customer** — each Iced Latte user maps to a Stripe Customer (created on first payment or registration)
2. **SetupIntent** — to save a card without charging it, create a SetupIntent. The frontend collects card details via Stripe Elements and confirms the SetupIntent. Stripe stores the card.
3. **PaymentMethod** — Stripe returns a `pm_xxx` ID. You store a reference (ID + last4 + brand + expiry) locally for display.
4. **Checkout with saved card** — when creating a Stripe Checkout Session, pass the `customer` ID and optionally a `payment_method` to pre-select.

**New entity:**

```
user_payment_method (NEW table)
├── id (UUID PK)
├── user_id (UUID FK → user_details)
├── stripe_payment_method_id (VARCHAR — Stripe's pm_xxx ID)
├── card_brand (VARCHAR — visa, mastercard, etc.)
├── card_last4 (VARCHAR(4))
├── card_exp_month (INT)
├── card_exp_year (INT)
├── is_default (BOOLEAN)
├── created_at (TIMESTAMPTZ)
```

**New endpoints (in payment module, not order):**

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/payment/setup-intent` | Create Stripe SetupIntent for saving a card |
| `GET` | `/api/v1/payment/methods` | List user's saved payment methods |
| `DELETE` | `/api/v1/payment/methods/{methodId}` | Remove a saved payment method |
| `PATCH` | `/api/v1/payment/methods/{methodId}/default` | Set as default payment method |

**Order creation integration:**

- `CreateNewOrderRequestDto` gets an optional `paymentMethodId` field
- If provided, the Stripe Checkout Session is created with that payment method pre-selected
- If not provided, Stripe shows its standard card entry form

**Stripe Customer mapping:**

- Add `stripe_customer_id` column to `user_details` table
- On first payment, create a Stripe Customer and store the ID
- All subsequent Stripe operations use this Customer ID

**Security considerations:**
- Never store full card numbers, CVVs, or PANs
- Only store Stripe's tokenized references (PaymentMethod ID) and display-safe metadata (last4, brand)
- Card data entry happens entirely in Stripe's frontend SDK — never touches your server

**Tests:**
- Unit test: Stripe SetupIntent creation
- Unit test: payment method listing and default selection
- Integration test: full flow — save card → create order with saved card → verify Stripe session uses the card

**Estimated effort:** 4–5 days

---

### Phase 6 — Order Cancellation & Refund Flow

**Goal:** Implement cancellation with a time window and a full refund lifecycle (request → approve → execute via Stripe → confirm via webhook).

**Why this matters:** Cancellation teaches business rule enforcement with time-based guards. Refunds teach the compensating transaction pattern — undoing a completed operation through a multi-step async process.

#### Cancellation

**Flow:**
1. User calls `POST /api/v1/orders/{orderId}/cancel`
2. `OrderCancellationService` checks:
   - Order belongs to the authenticated user
   - Order status is CREATED or PAID (cancellable states)
   - `OffsetDateTime.now().isBefore(order.getCancellationDeadline())`
3. If all guards pass, transition to CANCELLED via `OrderStatusTransitioner`
4. If order was PAID, automatically initiate a Stripe refund (as a side effect of cancellation — the order stays in CANCELLED status, not REFUND_REQUESTED)

**Cancellation window:**
- Configurable via `application.yml`: `order.cancellation-window-minutes: 30`
- Set on order creation: `order.setCancellationDeadline(OffsetDateTime.now().plusMinutes(windowMinutes))`
- After the window expires, user must use the refund flow instead

**New classes:**
```
order/
├── api/
│   └── OrderCancellationService.java
├── config/
│   └── OrderConfig.java                # @ConfigurationProperties for cancellation window
└── exception/
    └── OrderCancellationWindowExpiredException.java
```

#### Refund

**Flow:**
1. User calls `POST /api/v1/orders/{orderId}/refund` with optional `reason` field
2. `OrderRefundService` checks:
   - Order belongs to the authenticated user
   - Order status is PAID (refundable state — SHIPPED orders cannot be refunded directly; a return/dispute process would be needed, which is out of scope)
3. Transitions order to REFUND_REQUESTED
4. Calls Stripe Refund API (`Refund.create()`) with the original payment intent ID
   - Uses `@Retryable` with exponential backoff for resilience
5. Stripe processes the refund asynchronously
6. Stripe sends `charge.refunded` webhook event
7. `StripeWebhookService` handles the event, transitions order to REFUNDED via `OrderStatusTransitioner`

**New classes:**
```
order/
├── api/
│   └── OrderRefundService.java
payment/
├── api/
│   └── StripeRefundCreator.java        # Wraps Stripe Refund API call
```

**Stripe integration details:**
- Store `stripe_payment_intent_id` on the Order entity (captured from the Stripe Session on payment confirmation)
- Refund API call: `Refund.create(Map.of("payment_intent", order.getStripePaymentIntentId()))`
- Handle partial refunds in the future by adding an `amount` parameter (out of scope for now — full refunds only)

**New DB columns on `orders`:**
```sql
ALTER TABLE public.orders ADD COLUMN stripe_payment_intent_id VARCHAR(255);
ALTER TABLE public.orders ADD COLUMN refund_reason VARCHAR(500);
ALTER TABLE public.orders ADD COLUMN refunded_at TIMESTAMPTZ;
```

**Tests:**
- Unit test: cancellation within window succeeds
- Unit test: cancellation after window expires throws exception
- Unit test: cancellation of non-cancellable status throws exception
- Unit test: refund request transitions to REFUND_REQUESTED
- Unit test: Stripe refund API call with retry on failure
- Integration test: full cancellation flow — create order → pay → cancel within window → verify CANCELLED + Stripe refund initiated
- Integration test: full refund flow — create order → pay → request refund → simulate webhook → verify REFUNDED

**Estimated effort:** 4–5 days

---

### Phase 7 — Idempotency, Re-order, Admin Endpoints

**Goal:** Add distributed systems reliability (idempotency keys), a convenience feature (re-order), and admin tooling.

#### Idempotency keys

**Problem:** Double-click on "Place Order" button, network retry, or webhook + redirect both firing — all can create duplicate orders.

**Solution:** Client generates a UUID idempotency key and sends it in the `Idempotency-Key` header on `POST /api/v1/orders`.

**Implementation:**
- `OrderIdempotencyFilter` (or check in `OrderCreator`) — before creating an order, check if an order with this `idempotency_key` already exists for this user
- If found, return the existing order (200 OK, not 201) — the operation is idempotent
- If not found, create the order with the key stored
- The existing Stripe `(userId, sessionId)` idempotency check is a special case of this — unify both under the `idempotency_key` column

**New header:** `Idempotency-Key: <UUID>` (optional — if not provided, no idempotency check)

#### Re-order / Buy Again

**Flow:**
1. User calls `POST /api/v1/orders/{orderId}/reorder`
2. `OrderReorderService` fetches the original order's items
3. For each item, checks if the product still exists and is available
4. Adds available items to the user's shopping cart
5. Returns the updated cart (so the frontend can redirect to cart/checkout)
6. Unavailable items are listed in the response so the user knows what was skipped

**Response:**
```json
{
  "cartId": "...",
  "addedItems": 3,
  "unavailableItems": [
    { "productName": "Discontinued Blend", "reason": "Product no longer available" }
  ]
}
```

#### Admin endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/admin/orders` | Paginated list of all orders with filters (status, user, date range) |
| `PATCH` | `/api/v1/admin/orders/{orderId}/status` | Update order status (SHIP, DELIVER) |

**Admin authorization:**
- Secured with `@PreAuthorize("hasRole('ADMIN')")` or equivalent Spring Security check
- Admin status transitions: PAID → SHIPPED, SHIPPED → DELIVERED
- Request body for status update: `{ "event": "SHIP" }` — uses the same `OrderStatusTransitioner`

**Admin infrastructure prerequisites:**
- Add `ADMIN_ORDERS` constant to `ApiPaths`: `public static final String ADMIN_ORDERS = "/api/v1/admin/orders";` and corresponding pattern
- Add admin path to Spring Security config — `requestMatchers(ApiPaths.ADMIN_ORDERS_PATTERN).hasRole("ADMIN")`
- **Seed ADMIN authority data** — currently all seeded users have only `USER` authority. Need a migration to insert at least one user with `ADMIN` authority for testing:
  ```sql
  INSERT INTO public.user_granted_authority (id, user_id, authority)
  VALUES ('...', '11111111-1111-1111-1111-111111111111', 'ADMIN');
  ```
- Integration tests need a helper to register and authenticate an admin user

**Tests:**
- Unit test: idempotency key returns existing order on duplicate
- Unit test: re-order with mix of available and unavailable products
- Integration test: admin status transition PAID → SHIPPED → DELIVERED
- Integration test: non-admin user gets 403 on admin endpoints

**Estimated effort:** 3–4 days

---

### Phase 8 — Comprehensive Testing

**Goal:** Achieve production-grade test coverage with integration tests, concurrency tests, and edge case coverage.

**Integration tests (using Testcontainers + REST Assured):**

Following the project's existing pattern — extend `AuthenticatedUserIntegrationSupport`, use `registerAndAuthenticateUser()` for auth.

| Test class | Scenarios |
|-----------|-----------|
| `OrderLifecycleIntegrationTest` | Full happy path: add to cart → create order → pay (mock webhook) → ship (admin) → deliver (admin). Verify status at each step. Verify `order_status_history` records. |
| `OrderHistoryIntegrationTest` | Create 15+ orders across multiple statuses/dates. Test pagination, year filter, status filter, date range, sorting. Verify response structure matches paginated DTO. |
| `OrderCancellationIntegrationTest` | Cancel within window → success. Cancel after window → 409 Conflict. Cancel already-shipped order → 409. |
| `OrderRefundIntegrationTest` | Request refund on PAID order → REFUND_REQUESTED. Simulate Stripe webhook → REFUNDED. Request refund on CREATED order → 409. |
| `OrderAccessControlIntegrationTest` | User A cannot view User B's order (403). User A cannot cancel User B's order (403). Non-admin cannot use admin endpoints (403). |
| `OrderIdempotencyIntegrationTest` | Same idempotency key returns same order. Different key creates new order. |
| `OrderReorderIntegrationTest` | Re-order with all products available. Re-order with some products unavailable. Re-order from empty order. |

**Concurrency tests:**

```java
@Test
void concurrentStatusTransitions_onlyOneSucceeds() {
    // Create and pay for an order
    // Launch 2 threads: both try to cancel simultaneously
    // Assert: exactly one succeeds, the other gets OptimisticLockException (retried, then fails if state changed)
    // Verify: order is CANCELLED, status_history has exactly one CANCELLED entry
}
```

Use `ExecutorService` + `CountDownLatch` for synchronization, `Awaitility` for async assertions.

**Test data:**
- Create `OrderTestStub` in `order/stub/` with factory methods: `createOrder()`, `createOrderItem()`, `createOrderWithStatus(OrderStatus)`
- Use inline text blocks for REST Assured request bodies (newer project convention)

**Estimated effort:** 4–5 days

---

## 4. Final Package Structure

```
order/
├── endpoint/
│   ├── OrderEndpoint.java              # User-facing REST controller
│   └── AdminOrderEndpoint.java         # Admin REST controller
├── api/
│   ├── OrderCreator.java               # REWRITE — unified order creation
│   ├── OrderDetailProvider.java        # NEW — single order by ID
│   ├── OrderProvider.java              # EXISTING — lookup by userId+sessionId (isolation fix)
│   ├── OrdersProvider.java             # REWRITE — paginated + Specification-based
│   ├── OrderStateMachine.java          # NEW — transition table
│   ├── OrderStatusTransitioner.java    # NEW — executes transitions with guards
│   ├── OrderStatusHistoryRecorder.java # NEW — @EventListener for audit trail
│   ├── OrderCancellationService.java   # NEW — cancel with time window
│   ├── OrderRefundService.java         # NEW — refund request + Stripe call
│   └── OrderReorderService.java        # NEW — buy again
├── entity/
│   ├── Order.java                      # UPDATED — version, idempotencyKey, cancellationDeadline, etc.
│   ├── OrderItem.java                  # EXISTING — minor cleanup
│   └── OrderStatusHistory.java         # NEW — audit trail entity
├── repository/
│   ├── OrderRepository.java            # UPDATED — extends JpaSpecificationExecutor
│   └── OrderStatusHistoryRepository.java # NEW
├── converter/
│   └── OrderDtoConverter.java          # UPDATED — new DTO mappings
├── specification/
│   └── OrderSpecifications.java        # NEW — JPA Specifications for filtering
├── validator/
│   ├── OrderStatusTransitionValidator.java # NEW — guard conditions
│   ├── OrderAddressValidator.java      # NEW — address selection validation
│   └── OrderFilterValidator.java       # NEW — query parameter validation
├── config/
│   └── OrderConfig.java                # NEW — @ConfigurationProperties
├── event/
│   └── OrderStatusChangedEvent.java    # NEW — Spring ApplicationEvent
└── exception/
    ├── handler/
    │   └── OrderExceptionHandler.java  # UPDATED — handle new exceptions
    ├── InvalidOrderStateTransitionException.java  # NEW
    ├── OrderNotFoundException.java                # NEW
    ├── OrderAccessDeniedException.java            # NEW
    └── OrderCancellationWindowExpiredException.java # NEW
```

---

## 5. Dependencies

**Already in the project (no new additions needed):**

| Dependency | Usage in order module |
|-----------|----------------------|
| Spring Data JPA + Specifications | Dynamic query filtering for order history |
| Spring ApplicationEventPublisher | Domain events on status changes |
| Spring Retry (`@Retryable`) | Retry on optimistic lock conflicts and Stripe API failures |
| Spring Security (`@PreAuthorize`) | Admin endpoint authorization |
| MapStruct | Entity ↔ DTO conversion |
| Stripe SDK | Refund API, SetupIntent API, saved payment methods |
| Testcontainers + REST Assured | Integration tests |

**Recommended additions:**

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Awaitility | 4.2+ | Clean async assertions in concurrency tests |

No other new dependencies. The value is in the patterns, not the libraries.

---

## 6. Configuration

New properties in `application.yml`:

```yaml
# Order-specific config (new @ConfigurationProperties class)
order:
  cancellation-window-minutes: 30

# Integrate with existing PaginationConfig (add orders section)
pagination:
  orders:
    default-page-size: 10
    max-page-size: 50
    default-sort-attribute: createdAt
    default-sort-direction: desc
```

---

## 7. Timeline Summary

| Phase | Description | Effort | Depends on |
|-------|-------------|--------|------------|
| 1 | Fix bugs & schema foundation | 3–4 days | — |
| 2 | State machine & status transitions | 4–5 days | Phase 1 |
| 3 | Paginated order history with filters | 3–4 days | Phase 1 |
| 4 | Delivery address book integration | 2–3 days | Phase 1 |
| 5 | Stripe saved payment methods | 4–5 days | Phase 2 |
| 6 | Cancellation & refund flow | 4–5 days | Phase 2, 5 |
| 7 | Idempotency, re-order, admin endpoints | 3–4 days | Phase 2, 3 |
| 8 | Comprehensive testing | 4–5 days | All phases |
| **Total** | | **27–35 days** | |

Phases 3 and 4 can run in parallel with Phase 2 (they depend only on Phase 1).
Phase 5 can start after Phase 2.
Phase 8 is incremental — write tests as you complete each phase, with a final pass at the end.

---

## 8. Patterns & Skills Gained

| Pattern | Where it's applied |
|---------|-------------------|
| State machine design | `OrderStateMachine` — explicit transition table with guards |
| Domain events | `OrderStatusChangedEvent` via `ApplicationEventPublisher` |
| Optimistic locking | `@Version` on Order entity, retry on conflict |
| JPA Specifications | Dynamic filtering for order history |
| Compensating transactions | Refund flow — undo a completed payment |
| Idempotency keys | Prevent duplicate order creation |
| Time-based business rules | Cancellation window enforcement |
| Offset pagination with dynamic filtering | Paginated order history API with JPA Specifications |
| PCI-aware payment integration | Stripe saved cards via SetupIntents |
| Event sourcing (lightweight) | `order_status_history` audit trail |
| Concurrency testing | Verify optimistic locking under contention |

---

## 9. Frontend Impact Analysis

> **Frontend repo:** `/Users/zufar/IdeaProjects/Iced-Latte-Frontend`
> **Stack:** Next.js 16.2 (App Router), React 19, TypeScript, Zustand, Axios, Tailwind CSS

### Current frontend order flow

```
Cart page (/cart)
  └→ "Go to checkout →"
      └→ Checkout page (/checkout)  [requires auth]
          ├── CheckoutForm: recipientName, recipientSurname, recipientPhone
          ├── AddressPicker: select saved address OR enter new address fields
          └── CheckoutSummary: reads cart items + totalPrice from Zustand cartStore
              └→ "Place order" button
                  └→ POST /orders { recipientName, recipientSurname, recipientPhone?, address }
                      └→ on success: resetCart() + router.push('/orders')

Orders page (/orders)
  ├── Filter tabs: All | Placed (CREATED) | Paid (PAID) | On the way (DELIVERY) | Delivered (FINISHED)
  ├── GET /orders  or  GET /orders?status=STATUS
  └── OrderCard: expandable, shows items + delivery address

Orders success page (/orders/success?sessionId=...)  [LEGACY/DORMANT]
  └→ GET /payment/order?sessionId=...  (Stripe session verification)
  └→ Shows confirmation email, resets cart
  └→ This page is NEVER reached by the current checkout flow
```

### Critical frontend findings

1. **No Stripe client-side integration** — `@stripe/stripe-js` is NOT in `package.json`. No Stripe Elements, no Stripe Checkout redirect, no payment intent handling. The checkout flow calls `POST /orders` directly without any payment step.

2. **The Stripe payment flow is dormant** — The backend has `POST /api/v1/payment` (creates Stripe Checkout Session) and `GET /api/v1/payment/order` (verifies session), but the frontend never calls `POST /payment`. The `/orders/success` page exists but is unreachable from the current checkout flow.

3. **Frontend already has AddressPicker** — The checkout form already supports selecting a saved delivery address OR entering a new one via `AddressPicker` component. However, the `POST /orders` request always sends an inline `address` object — it does NOT send `deliveryAddressId` even when a saved address is selected. The frontend converts the saved address back to inline fields.

4. **Status values are hardcoded** — `OrderStatusBadge` maps: `CREATED` → "Order placed", `PAID` → "Paid", `DELIVERY` → "On the way", `FINISHED` → "Delivered". The `Order` TypeScript type has `status: 'CREATED' | 'PAID' | 'DELIVERY' | 'FINISHED'`.

5. **No pagination** — `fetchOrders()` returns the full array. `OrderHistory` renders all orders. No infinite scroll, no page controls.

6. **No order detail page** — No `/orders/{orderId}` route. Orders are shown as expandable cards in the list.

7. **No cancel/refund UI** — No buttons or flows for cancellation or refund.

8. **No idempotency protection** — The "Place order" button disables during `loading` state, but there's no `Idempotency-Key` header. A network retry or double-click race could create duplicates.

9. **Cart is cleared client-side** — `useCheckoutForm` calls `resetCart()` (Zustand store reset) on success. The backend `POST /orders` does NOT clear the cart (bug #2). So the cart is only cleared in the browser, not on the server.

### Required frontend changes per phase

**Phase 1 (bug fixes):**
- Backend now clears the cart on `POST /orders`. Frontend `resetCart()` call is still needed (clears Zustand store + localStorage for guest cart), but the server-side cart is now also cleared. No frontend code change needed — existing behavior is compatible.
- **Note:** The frontend's `createOrder()` in `ordersApi.ts` returns `Promise<void>` — it ignores the response body entirely. The backend returns `OrderDto` (201), but the frontend doesn't read it. This means backend response shape changes (e.g., `OrderDto` → `OrderDetailDto`) won't break the create flow. However, Phase 5 (Stripe) will need the response to get the order ID for the payment step, so `createOrder()` will need to return the response body at that point.

**Phase 2 (state machine):**
- No immediate frontend change needed. The status values don't change yet in Phase 2 (that's the data migration in Phase 4 of DB migrations, coordinated with the backward compatibility strategy).

**Phase 3 (paginated order history) — BREAKING:**
- **`GET /orders` response changes** from `OrderDto[]` to `OrderPageDto { content: OrderSummaryDto[], page, size, totalElements, totalPages }`. The frontend `fetchOrders()` in `src/features/orders/api/ordersApi.ts` and `useOrders` hook in `src/features/orders/hooks/useOrders.ts` must be updated to:
  - Send `page`, `size`, `sortBy`, `sortDirection` query parameters
  - Read `response.data.content` instead of `response.data`
  - Handle pagination metadata for UI controls
- **`OrderHistory` component** needs pagination controls (or infinite scroll) and year/date range filters
- **`OrderSummaryDto` has different fields** than the current `OrderDto` — it has `firstItemName` and `firstItemImageUrl` instead of the full `items` array. `OrderCard` must be updated to show the summary in list view and fetch full details on expand (via `GET /orders/{orderId}`)
- **New `GET /orders/{orderId}` endpoint** — `OrderCard` expand should call this instead of relying on the list response having full item details
- **TypeScript types** in `src/features/orders/types/orderTypes.ts` must be updated for `OrderSummaryDto`, `OrderDetailDto`, `OrderPageDto`

**Phase 4 (delivery address book) — MINOR:**
- Frontend `AddressPicker` already exists and works. The change is: when a saved address is selected, send `deliveryAddressId` instead of converting to inline `address` fields. Update `useCheckoutForm` to check if a saved address is selected and send the appropriate field.
- The `CreateOrderRequest` TypeScript type needs `deliveryAddressId?: string` added.

**Phase 5 (Stripe saved payment methods) — MAJOR:**
- **Add `@stripe/stripe-js` dependency** to `package.json`
- **New payment method management UI** in user profile (list saved cards, add new card via Stripe Elements, delete, set default)
- **Checkout flow changes**: after `POST /orders` creates the order, redirect to Stripe Checkout (or embed Stripe Elements) for payment. On payment success, Stripe redirects to `/orders/success?sessionId=...` which already exists but needs to be wired up.
- **The dormant `/orders/success` page becomes active** — it already calls `GET /payment/order?sessionId=...` and resets the cart. Needs updates: the frontend `PaymentSessionStatus` type declares a `status` field that doesn't exist in the backend `PaymentConfirmationEmail` DTO (which only has `customerEmail`). Fix the type and adapt the component to the new order-first flow.
- **Payment method selection in checkout** — add a card picker component to `CheckoutForm`

**Phase 6 (cancellation & refund) — NEW UI:**
- **Cancel button** on `OrderCard` — visible only when `canCancel` is `true` in `OrderDetailDto`. Calls `POST /orders/{orderId}/cancel`. Show confirmation dialog.
- **Refund button** on `OrderCard` — visible only when `canRefund` is `true`. Shows a form for optional reason. Calls `POST /orders/{orderId}/refund`.
- **New status badges** in `OrderStatusBadge`: `CANCELLED` → "Cancelled" (red/gray), `REFUND_REQUESTED` → "Refund requested" (orange), `REFUNDED` → "Refunded" (purple). Also rename: `DELIVERY` → `SHIPPED` ("Shipped"), `FINISHED` → `DELIVERED` ("Delivered").
- **Cancellation deadline display** — show countdown or deadline time on cancellable orders

**Phase 7 (idempotency, re-order, admin):**
- **Idempotency key** — generate `crypto.randomUUID()` before calling `POST /orders`, send as `Idempotency-Key` header. Store in component state to prevent regeneration on retry.
  - **Proxy update required:** The Next.js API proxy (`src/app/api/proxy/[...path]/route.ts`) only forwards a whitelist of headers (`X-Session-ID`, `X-Trace-ID`, `X-Correlation-ID`). `Idempotency-Key` must be added to the `FORWARDED_HEADERS` array, otherwise it will be silently dropped before reaching the backend.
- **Re-order button** on `OrderCard` — calls `POST /orders/{orderId}/reorder`, shows toast with added/unavailable items, redirects to `/cart`
- **Admin UI** — out of scope for the frontend plan (could be a separate admin dashboard or added to a profile section for admin users)

### Frontend file impact summary

| Frontend file | Phase | Change |
|---|---|---|
| `src/features/orders/types/orderTypes.ts` | 3, 6 | New types: `OrderSummaryDto`, `OrderDetailDto`, `OrderPageDto`, expanded `OrderStatus` union |
| `src/features/orders/api/ordersApi.ts` | 3, 6, 7 | Add pagination params, add `fetchOrder(id)`, `cancelOrder(id)`, `refundOrder(id, reason?)`, `reorder(id)` |
| `src/features/orders/hooks/useOrders.ts` | 3 | Handle paginated response, add page state |
| `src/features/orders/components/OrderHistory.tsx` | 3 | Add pagination controls, year/date filters |
| `src/features/orders/components/OrderCard.tsx` | 3, 6, 7 | Fetch detail on expand, add cancel/refund/reorder buttons |
| `src/features/orders/components/OrderStatusBadge.tsx` | 6 | Add SHIPPED, DELIVERED, CANCELLED, REFUND_REQUESTED, REFUNDED badges |
| `src/features/orders/components/OrderSuccess.tsx` | 5 | Wire up to new Stripe checkout flow |
| `src/features/checkout/hooks/useCheckoutForm.ts` | 4, 5, 7 | Send `deliveryAddressId`, add payment step, add idempotency key |
| `src/features/checkout/types/checkoutTypes.ts` | 4, 5 | Add `deliveryAddressId`, `paymentMethodId` |
| `src/features/checkout/components/CheckoutForm.tsx` | 5 | Add payment method picker |
| `package.json` | 5 | Add `@stripe/stripe-js` |
| `src/app/api/proxy/[...path]/route.ts` | 7 | Add `Idempotency-Key` to `FORWARDED_HEADERS` whitelist |

---

## 10. User Flow Walkthrough — All Scenarios & Edge Cases

This section traces every user interaction end-to-end: **browser → frontend → Next.js proxy → backend → database**, identifying gaps in the plan.

---

### Flow 1: Place Order (happy path)

```
1. User on /cart clicks "Go to checkout →"
   FE: router.push('/checkout') (or /signin?next=/checkout if guest)

2. User on /checkout fills recipient info, picks saved address (or enters new)
   FE: CheckoutForm renders AddressPicker + recipient fields
   FE: CheckoutSummary reads tempItems + totalPrice from Zustand cartStore

3. User clicks "Place order"
   FE: useCheckoutForm.handleSubmit()
     → generates Idempotency-Key: crypto.randomUUID()                [Phase 7]
     → resolves address: deliveryAddressId (saved) OR inline address  [Phase 4]
     → POST /api/proxy/orders
       Headers: { Authorization: Bearer <token>, Idempotency-Key: <uuid> }
       Body: { recipientName, recipientSurname, recipientPhone?,
               deliveryAddressId? | address? }

4. Next.js proxy forwards to backend
   Proxy: /api/proxy/orders → POST http://localhost:8083/api/v1/orders
   ⚠ Proxy must forward Idempotency-Key header                       [Phase 7]

5. Backend: OrderEndpoint.createOrder()
   → SecurityPrincipalProvider.getUserId() → extracts userId from JWT
   → OrderCreator.create(userId, request)
     a. Check idempotency_key: if exists for this user → return existing order (200)
     b. Validate address: OrderAddressValidator checks exactly one of
        deliveryAddressId / address
     c. If deliveryAddressId: fetch DeliveryAddressEntity, verify belongs to user,
        snapshot into Address
     d. If inline address: build Address from request fields
     e. Fetch cart: shoppingCartService.getByUserIdOrThrow(userId)
     f. Validate cart not empty
     g. Convert cart items → OrderItem list (snapshot product name, price, quantity)
     h. Build Order: status=CREATED, set cancellationDeadline, set idempotencyKey
     i. Save order (cascade saves OrderItem + Address)
     j. Publish OrderStatusChangedEvent(null → CREATED)              [Phase 2]
     k. Delete cart: shoppingCartRepository.deleteByUserId(userId)   [Phase 1 fix]
     l. Return OrderDetailDto (201 Created)

6. Database writes:
   INSERT INTO orders (status='CREATED', version=0, idempotency_key, ...)
   INSERT INTO order_item (...) × N
   INSERT INTO address (...)  — snapshot
   INSERT INTO order_status_history (old_status=null, new_status='CREATED')
   DELETE FROM shopping_cart_item WHERE shopping_cart_id = ...
   DELETE FROM shopping_cart WHERE user_id = ...

7. Frontend receives 201
   FE: resetCart() — clears Zustand store + localStorage
   FE: router.push('/orders')
```

**Edge cases:**

| Edge case | What happens | Covered? |
|-----------|-------------|----------|
| Empty cart | Backend throws 400 → FE shows error | ✅ Phase 1 |
| Neither address nor deliveryAddressId | Validator rejects (400) | ✅ Phase 4 |
| Both address AND deliveryAddressId | Validator rejects (400) | ✅ Phase 4 |
| deliveryAddressId doesn't exist | 404 | ✅ Phase 4 |
| deliveryAddressId belongs to another user | 404 (don't reveal existence) | ✅ Phase 4 |
| Duplicate idempotency key (double-click) | Returns existing order (200) | ✅ Phase 7 |
| No idempotency key sent | Order created normally, no dedup | ✅ Phase 7 |
| JWT expired mid-checkout | Proxy returns 401 → FE redirects to /signin | ✅ Existing |
| Product price changed since cart add | Order snapshots price from cart. Price at cart-add time is used. | ✅ Existing |
| Product deleted since cart add | Order still created with stale data. No stock check. | ⚠ **GAP #1** |
| Network timeout on POST /orders | FE shows error. Retry with same idempotency key → safe. | ✅ Phase 7 |

---

### Flow 2: Pay for Order (Stripe)

```
1. After order creation, FE has OrderDetailDto with order.id
   ⚠ createOrder() currently returns void — must return response body [Section 9]

2. FE calls POST /api/proxy/payment with order ID
   ⚠ NEW frontend call — currently FE never calls POST /payment

3. Backend: StripeSessionCreator.createSession()
   → Fetches Order by ID (not cart — Phase 2 change)
   → Builds Stripe line items from OrderItem list
   → Creates Stripe Checkout Session: metadata = { userId, orderId }
   → Returns { sessionId, clientSecret }

4. FE redirects to Stripe Checkout
   ⚠ Requires @stripe/stripe-js in package.json                      [Phase 5]

5. User completes payment on Stripe

6. Stripe sends checkout.session.completed webhook
   Backend: StripeWebhookService.processWebhook()
     → handleCompleted(session)
       → Extract orderId from session metadata
       → OrderStatusTransitioner.transition(orderId, PAYMENT_CONFIRMED)
         → CREATED → PAID
       → Store stripe_payment_intent_id on Order (for future refunds)
       → PaymentEmailConfirmation.send(session)

7. Stripe redirects user to /orders/success?sessionId=...
   FE: OrderSuccess component
     → GET /payment/order?sessionId=...
     → Shows confirmation, calls resetCart()

8. Database:
   UPDATE orders SET status='PAID', stripe_payment_intent_id='pi_...'
   INSERT INTO order_status_history (CREATED → PAID)
```

**Edge cases:**

| Edge case | What happens | Covered? |
|-----------|-------------|----------|
| User abandons Stripe checkout | Order stays CREATED. No webhook. User can retry or cancel. | ✅ |
| Webhook + redirect fire simultaneously | Optimistic locking → one succeeds, other retries and sees PAID. | ✅ Phase 2 |
| Payment fails on Stripe | checkout.session.expired event. Order stays CREATED. | ✅ Existing |
| User closes browser after payment | Webhook still fires → PAID. User sees it on next visit. | ✅ |
| Order CANCELLED before webhook arrives | State machine rejects PAYMENT_CONFIRMED on CANCELLED. Money collected but order cancelled. | ⚠ **GAP #2** — need auto-refund |
| CREATED order never paid, sits forever | No expiration mechanism. | ⚠ **GAP #3** — need order expiration |

---

### Flow 3: View Order History

```
1. User navigates to /orders
   FE: useOrders hook fires

2. FE: GET /orders?page=0&size=10&sortBy=createdAt&sortDirection=DESC
   (+ optional: &status=PAID&year=2026)

3. Backend: OrdersProvider builds Specification, queries with Pageable
   → Returns OrderPageDto { content: OrderSummaryDto[], page, size, totalElements, totalPages }

4. FE renders OrderCard list + pagination controls
```

**Edge cases:**

| Edge case | What happens | Covered? |
|-----------|-------------|----------|
| 0 orders | Empty page → FE shows "No orders yet" | ✅ Existing FE |
| 500 orders | Paginated, 10/page | ✅ Phase 3 |
| Invalid page/size params | PaginationParametersValidator rejects (400) | ✅ Phase 3 |
| dateFrom > dateTo | OrderFilterValidator rejects (400) | ✅ Phase 3 |
| Filter returns 0 results | Empty content array, FE shows empty state | ✅ |

---

### Flow 4: View Single Order Detail

```
1. User expands OrderCard
   FE: GET /orders/{orderId}

2. Backend: OrderDetailProvider
   → findById → if not found: 404
   → if order.userId != authenticated userId: 403
   → Map to OrderDetailDto (items, address, canCancel, canRefund, cancellationDeadline)
   → canCancel = status in [CREATED, PAID] AND now < cancellationDeadline
   → canRefund = status == PAID

3. FE renders items, address, action buttons based on canCancel/canRefund
```

**Edge cases:**

| Edge case | What happens | Covered? |
|-----------|-------------|----------|
| Order not found | 404 | ✅ Phase 3 |
| Another user's order | 403 | ✅ Phase 3 |
| No delivery address (legacy Stripe path bug) | deliveryAddress null → FE handles with `&&` check | ✅ Existing FE |

---

### Flow 5: Cancel Order

```
1. User sees "Cancel" button (canCancel=true), clicks it
   FE: confirmation dialog → POST /orders/{orderId}/cancel

2. Backend: OrderCancellationService
   → Verify ownership, check status CREATED|PAID, check deadline
   → Transition to CANCELLED
   → If was PAID: call StripeRefundCreator.refund(stripePaymentIntentId)

3. FE: update order card to CANCELLED, hide action buttons
```

**Edge cases:**

| Edge case | What happens | Covered? |
|-----------|-------------|----------|
| Cancel after deadline | 409 → FE shows error | ✅ Phase 6 |
| Cancel already-cancelled order | 409 (no CANCEL transition from CANCELLED) | ✅ |
| Cancel SHIPPED order | 409 (no CANCEL transition from SHIPPED) | ✅ |
| Cancel PAID order but Stripe refund fails | Order is CANCELLED, money not returned | ⚠ **GAP #4** |
| Concurrent cancel attempts | Optimistic locking → one wins | ✅ Phase 2 |
| Cancel another user's order | 403 | ✅ |

---

### Flow 6: Request Refund

```
1. User sees "Request refund" button (canRefund=true), clicks it
   FE: shows reason textarea → POST /orders/{orderId}/refund { reason }

2. Backend: OrderRefundService
   → Verify ownership, check status PAID
   → Transition to REFUND_REQUESTED, store reason
   → Call StripeRefundCreator.refund() with @Retryable

3. Stripe processes refund async → charge.refunded webhook
   → Backend: look up order by stripe_payment_intent_id
   → Transition REFUND_REQUESTED → REFUNDED, set refunded_at

4. FE: order shows REFUND_REQUESTED badge. On next page load after webhook: REFUNDED.
```

**Edge cases:**

| Edge case | What happens | Covered? |
|-----------|-------------|----------|
| Refund on CREATED order | 409 | ✅ |
| Refund on SHIPPED order | 409 | ✅ |
| Refund on already-refunded order | 409 | ✅ |
| Stripe refund API fails after all retries | Order stuck in REFUND_REQUESTED, money not returned | ⚠ **GAP #5** |
| charge.refunded webhook never arrives | Order stuck in REFUND_REQUESTED forever | ⚠ **GAP #5** |
| Webhook lookup: charge.refunded has payment_intent, not orderId | Must look up by stripe_payment_intent_id column | ⚠ **GAP #6** |
| User requests refund then tries to cancel | 409 (REFUND_REQUESTED has no CANCEL) | ✅ |

---

### Flow 7: Re-order / Buy Again

```
1. User clicks "Buy again" on a past order
   FE: POST /orders/{orderId}/reorder

2. Backend: OrderReorderService
   → Fetch order items, check each product exists
   → Add available items to cart
   → Return { cartId, addedItems, unavailableItems }

3. FE: show toast, refresh cart, redirect to /cart
```

**Edge cases:**

| Edge case | What happens | Covered? |
|-----------|-------------|----------|
| All products unavailable | addedItems=0, FE shows message | ✅ |
| Product already in cart | Cart `mergeIntoCart()` increments qty for existing products. No duplicate. | ✅ Verified |
| Re-order another user's order | 403 | ✅ |
| Product price changed | Added at current price (live ProductInfo) | ✅ |

---

### Flow 8: View Status History

```
1. User clicks "View history" on order detail
   FE: GET /orders/{orderId}/history

2. Backend: returns List<OrderStatusHistoryDto> ordered by changedAt

3. FE: renders timeline of status changes with timestamps
```

No significant edge cases beyond auth (403 for other user's order).

---

### Flow 9: Admin Ships/Delivers Order

```
1. Admin: GET /admin/orders?status=PAID
2. Admin: PATCH /admin/orders/{orderId}/status { event: "SHIP" }
3. Backend: @PreAuthorize("hasRole('ADMIN')"), transition PAID → SHIPPED
4. Later: PATCH /admin/orders/{orderId}/status { event: "DELIVER" }
5. Backend: transition SHIPPED → DELIVERED
```

**Edge cases:**

| Edge case | What happens | Covered? |
|-----------|-------------|----------|
| Non-admin calls admin endpoint | 403 | ✅ |
| Ship a CREATED order | 409 (no SHIP from CREATED) | ✅ |
| Skip SHIPPED, go straight to DELIVERED | 409 (no DELIVER from PAID) | ✅ |

---

### Flow 10: Guest User → Checkout

```
1. Guest adds to cart (localStorage)
2. Clicks "Go to checkout" → redirected to /signin
3. Signs in → cart syncs (POST /cart/items merges localStorage → server)
4. Proceeds to /checkout → Flow 1
```

No order-specific edge cases.

---

### Gaps Summary

| # | Gap | Severity | Fix | Phase |
|---|-----|----------|-----|-------|
| 1 | No stock/availability check at order creation | Medium | Validate each cart item's product exists and is active before creating order. Return 409 with list of unavailable items. | 1 |
| 2 | Payment arrives for cancelled order | High | In webhook: if order is CANCELLED, auto-refund via Stripe instead of rejecting transition. Log for audit. | 6 |
| 3 | Unpaid CREATED orders never expire | Medium | Scheduled job cancels CREATED orders older than N hours (configurable, e.g., 24h). Or rely on Stripe session expiration (default 24h) + `checkout.session.expired` webhook. | 2 |
| 4 | Stripe refund fails after cancellation | High | Option A: transition to CANCELLED only after refund confirmed (async cancel). Option B: transition immediately, retry refund with dead-letter queue + admin alert. Recommend B. | 6 |
| 5 | Stuck REFUND_REQUESTED (webhook never arrives / API fails) | Medium | Scheduled job polls Stripe for refund status on orders in REFUND_REQUESTED > N hours. Or admin monitoring dashboard. | 6 |
| 6 | charge.refunded webhook has payment_intent, not orderId | Medium | Look up order by `stripe_payment_intent_id` column. Index added to Migration 2 (`idx_orders_stripe_payment_intent_id`). Add `findByStripePaymentIntentId()` to OrderRepository. Add case to webhook handler. | 6 |
| 7 | Re-order may create duplicate cart items | Low | **Verified: not a gap.** Cart's `mergeIntoCart()` already increments quantity for existing products via `increaseExistingItemQuantities()`. Unique constraint `uq_shopping_cart_item_cart_product` is a safety net. Just add an integration test. | 7 |

---

## Reference Documents

- [OpenAPI Spec Design](order-module-openapi-spec.md) — full endpoint and schema definitions
- [Database Migration Design](order-module-db-migrations.md) — all SQL migration scripts

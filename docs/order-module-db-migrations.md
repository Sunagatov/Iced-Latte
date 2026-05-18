# Order Module — Database Migration Design Reference

> **Parent document:** [Order Module Implementation Plan](order-module-plan.md)
> **Migration location:** `src/main/resources/db/changelog/version-2.0/`
> **Naming convention:** `DD.MM.YYYY.partN.description.sql`

---

## Existing Schema (as-is)

```sql
-- orders table (created 07.01.2024)
CREATE TABLE public.orders (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL,                    -- NO FK to user_details
    session_id         VARCHAR(255) NOT NULL,
    created_at         TIMESTAMPTZ DEFAULT current_timestamp,
    status             VARCHAR(55) NOT NULL,             -- CREATED, PAID, DELIVERY, FINISHED
    items_quantity      INT NOT NULL CHECK (items_quantity >= 0),
    address_id         UUID NOT NULL,                    -- FK to address (legacy profile address)
    items_total_price  DECIMAL NOT NULL CHECK (items_total_price > 0),
    recipient_name     VARCHAR(128) NOT NULL DEFAULT '',
    recipient_surname  VARCHAR(128) NOT NULL DEFAULT '',
    recipient_phone    VARCHAR(32),
    -- audit columns added 25.01.2025:
    updated_at         TIMESTAMPTZ,
    created_by         UUID,
    updated_by         UUID
);

-- order_item table (created 07.01.2024)
CREATE TABLE public.order_item (
    id                UUID PRIMARY KEY,
    order_id          UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id        UUID NOT NULL,                     -- NO FK to product (intentional snapshot)
    product_price     DECIMAL NOT NULL CHECK (product_price > 0),
    product_name      VARCHAR(64) NOT NULL,
    products_quantity INT NOT NULL CHECK (products_quantity >= 0)
);
```

**Issues with current schema:**
- No index on `orders.user_id` (full table scan on `findAllByUserId`)
- No index on `order_item.order_id` (slow JOINs)
- No FK on `orders.user_id` to `user_details`
- No optimistic locking column
- No idempotency key column
- `address_id` references legacy `address` table instead of `delivery_address`

---

## Migration Scripts

### Migration 1 — Add indexes and FK constraint

**File:** `03.05.2026.part1.add-order-indexes-and-fk.sql`

```sql
-- Index on orders.user_id for findAllByUserId queries
CREATE INDEX IF NOT EXISTS idx_orders_user_id
    ON public.orders (user_id);

-- Index on order_item.order_id for JOIN performance
CREATE INDEX IF NOT EXISTS idx_order_item_order_id
    ON public.order_item (order_id);

-- FK constraint: orders.user_id → user_details.id
ALTER TABLE public.orders
    ADD CONSTRAINT fk_orders_user
    FOREIGN KEY (user_id) REFERENCES public.user_details(id);

-- Composite index for status filtering queries
CREATE INDEX IF NOT EXISTS idx_orders_user_id_status
    ON public.orders (user_id, status);

-- Index for idempotency lookups
CREATE INDEX IF NOT EXISTS idx_orders_user_id_session_id
    ON public.orders (user_id, session_id);
```

---

### Migration 2 — Add new columns to orders table

**File:** `03.05.2026.part2.add-order-lifecycle-columns.sql`

```sql
-- Optimistic locking
ALTER TABLE public.orders
    ADD COLUMN version INT NOT NULL DEFAULT 0;

-- Idempotency key (unique per order, nullable for legacy orders)
ALTER TABLE public.orders
    ADD COLUMN idempotency_key VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_idempotency_key
    ON public.orders (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Cancellation deadline
ALTER TABLE public.orders
    ADD COLUMN cancellation_deadline TIMESTAMPTZ;

-- Stripe payment intent ID (needed for refunds)
ALTER TABLE public.orders
    ADD COLUMN stripe_payment_intent_id VARCHAR(255);

-- Refund metadata
ALTER TABLE public.orders
    ADD COLUMN refund_reason VARCHAR(500);

ALTER TABLE public.orders
    ADD COLUMN refunded_at TIMESTAMPTZ;

-- Index for refund webhook lookup (charge.refunded event contains payment_intent ID)
CREATE INDEX IF NOT EXISTS idx_orders_stripe_payment_intent_id
    ON public.orders (stripe_payment_intent_id)
    WHERE stripe_payment_intent_id IS NOT NULL;
```

---

### Migration 3 — Create order_status_history table

**File:** `03.05.2026.part3.create-order-status-history-table.sql`

```sql
CREATE TABLE IF NOT EXISTS public.order_status_history (
    id          UUID PRIMARY KEY,
    order_id    UUID NOT NULL,
    old_status  VARCHAR(55),
    new_status  VARCHAR(55) NOT NULL,
    changed_by  UUID,
    reason      VARCHAR(500),
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,

    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id)
        REFERENCES public.orders(id)
        ON DELETE CASCADE
);

-- Index for fetching history by order
CREATE INDEX IF NOT EXISTS idx_order_status_history_order_id
    ON public.order_status_history (order_id);

-- Index for chronological ordering
CREATE INDEX IF NOT EXISTS idx_order_status_history_changed_at
    ON public.order_status_history (order_id, changed_at);
```

---

### Migration 4 — Migrate order status values

**File:** `03.05.2026.part4.migrate-order-status-values.sql`

```sql
-- Rename DELIVERY → SHIPPED, FINISHED → DELIVERED
-- This is a data migration, not a schema change

UPDATE public.orders
SET status = 'SHIPPED'
WHERE status = 'DELIVERY';

UPDATE public.orders
SET status = 'DELIVERED'
WHERE status = 'FINISHED';
```

**Note:** Run this migration after deploying code that accepts both old and new status values. The Java enum should temporarily include both old and new values during the transition period.

---

### Migration 4b — Seed ADMIN authority for testing

**File:** `03.05.2026.part4b.insert-admin-authority.sql`

```sql
-- Add ADMIN authority to the first seeded user for admin endpoint testing
-- Currently all seeded users only have USER authority
INSERT INTO public.user_granted_authority (id, user_id, authority)
VALUES ('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1c1', '11111111-1111-1111-1111-111111111111', 'ADMIN')
ON CONFLICT DO NOTHING;
```

**Note:** This gives the seeded integration-test account id (`11111111-...`) both USER and ADMIN roles. Required for admin endpoint integration tests. In production, admin users would be provisioned through a separate process.

---

### Migration 4c — Migrate address_id FK (deferred to Phase 4)

**File:** `03.05.2026.part4c.migrate-order-address-fk.sql`

This migration is complex because existing orders reference the legacy `address` table (which is the user's profile address, shared 1:1 with `user_details`). The new design should snapshot addresses into the order so they're independent of the user's profile.

```sql
-- Step 1: Drop the existing FK constraint to the legacy address table
ALTER TABLE public.orders
    DROP CONSTRAINT IF EXISTS fk_address;

-- Step 2: The address_id column now just references the address table as a snapshot store.
-- Existing orders keep their current address_id values (the Address rows already exist).
-- New orders will create new Address rows as snapshots (current behavior via CascadeType.ALL).
--
-- No data migration needed — the Order entity already uses @OneToOne with CascadeType.ALL,
-- which creates a new Address row for each order. The old FK just ensured referential integrity
-- to the address table, which still holds.

-- Step 3: Re-add FK without ON DELETE CASCADE (we don't want deleting a user's profile
-- address to cascade-delete order address snapshots)
ALTER TABLE public.orders
    ADD CONSTRAINT fk_order_address
    FOREIGN KEY (address_id) REFERENCES public.address(id);
```

**Note:** This is listed in Phase 1 item 8 of the main plan but is deferred to Phase 4 (delivery address book integration) because the full solution depends on deciding whether orders should snapshot into the existing `address` table or a new dedicated table. The current `CascadeType.ALL` on the `Order.deliveryAddress` field already creates independent Address rows per order, so the main risk is the `ON DELETE CASCADE` on the existing FK.

---

### Migration 5 — Create user_payment_method table

**File:** `03.05.2026.part5.create-user-payment-method-table.sql`

```sql
CREATE TABLE IF NOT EXISTS public.user_payment_method (
    id                        UUID PRIMARY KEY,
    user_id                   UUID NOT NULL,
    stripe_payment_method_id  VARCHAR(255) NOT NULL,
    card_brand                VARCHAR(32) NOT NULL,
    card_last4                VARCHAR(4) NOT NULL,
    card_exp_month            INT NOT NULL CHECK (card_exp_month BETWEEN 1 AND 12),
    card_exp_year             INT NOT NULL CHECK (card_exp_year >= 2024),
    is_default                BOOLEAN NOT NULL DEFAULT false,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,

    CONSTRAINT fk_user_payment_method_user
        FOREIGN KEY (user_id)
        REFERENCES public.user_details(id)
        ON DELETE CASCADE
);

-- Index for listing payment methods by user
CREATE INDEX IF NOT EXISTS idx_user_payment_method_user_id
    ON public.user_payment_method (user_id);

-- Enforce at most one default payment method per user
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_payment_method_default
    ON public.user_payment_method (user_id)
    WHERE is_default = true;
```

---

### Migration 6 — Add stripe_customer_id to user_details

**File:** `03.05.2026.part6.add-stripe-customer-id-to-user.sql`

```sql
ALTER TABLE public.user_details
    ADD COLUMN stripe_customer_id VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_details_stripe_customer_id
    ON public.user_details (stripe_customer_id)
    WHERE stripe_customer_id IS NOT NULL;
```

---

## Changelog Registration

Append to the **existing** `src/main/resources/db/changelog/version-2.0/changelog-master-version-2.0.yaml` (which already contains 13 migration entries for AI summaries, auth sessions, constraints, etc.):

```yaml
  # ========== ORDER MODULE ENHANCEMENTS ==========
  - include:
      file: db/changelog/version-2.0/03.05.2026.part1.add-order-indexes-and-fk.sql
      errorIfMissing: true
  - include:
      file: db/changelog/version-2.0/03.05.2026.part2.add-order-lifecycle-columns.sql
      errorIfMissing: true
  - include:
      file: db/changelog/version-2.0/03.05.2026.part3.create-order-status-history-table.sql
      errorIfMissing: true
  - include:
      file: db/changelog/version-2.0/03.05.2026.part4.migrate-order-status-values.sql
      errorIfMissing: true
  - include:
      file: db/changelog/version-2.0/03.05.2026.part4b.insert-admin-authority.sql
      errorIfMissing: true
  - include:
      file: db/changelog/version-2.0/03.05.2026.part4c.migrate-order-address-fk.sql
      errorIfMissing: true
  - include:
      file: db/changelog/version-2.0/03.05.2026.part5.create-user-payment-method-table.sql
      errorIfMissing: true
  - include:
      file: db/changelog/version-2.0/03.05.2026.part6.add-stripe-customer-id-to-user.sql
      errorIfMissing: true
```

---

## Target Schema (after all migrations)

```
orders
├── id                        UUID PK
├── user_id                   UUID FK → user_details(id)     [NEW FK]
├── session_id                VARCHAR(255)
├── status                    VARCHAR(55)                     [expanded enum]
├── version                   INT DEFAULT 0                   [NEW]
├── idempotency_key           VARCHAR(64) UNIQUE              [NEW]
├── address_id                UUID FK → address(id)
├── recipient_name            VARCHAR(128)
├── recipient_surname         VARCHAR(128)
├── recipient_phone           VARCHAR(32)
├── items_quantity            INT
├── items_total_price         DECIMAL
├── cancellation_deadline     TIMESTAMPTZ                     [NEW]
├── stripe_payment_intent_id  VARCHAR(255)                    [NEW]
├── refund_reason             VARCHAR(500)                    [NEW]
├── refunded_at               TIMESTAMPTZ                     [NEW]
├── created_at                TIMESTAMPTZ
├── updated_at                TIMESTAMPTZ
├── created_by                UUID
└── updated_by                UUID

order_item (unchanged)
├── id                UUID PK
├── order_id          UUID FK → orders(id) ON DELETE CASCADE
├── product_id        UUID
├── product_price     DECIMAL
├── product_name      VARCHAR(64)
└── products_quantity INT

order_status_history [NEW]
├── id          UUID PK
├── order_id    UUID FK → orders(id) ON DELETE CASCADE
├── old_status  VARCHAR(55)
├── new_status  VARCHAR(55)
├── changed_by  UUID
├── reason      VARCHAR(500)
└── changed_at  TIMESTAMPTZ

user_payment_method [NEW]
├── id                        UUID PK
├── user_id                   UUID FK → user_details(id) ON DELETE CASCADE
├── stripe_payment_method_id  VARCHAR(255)
├── card_brand                VARCHAR(32)
├── card_last4                VARCHAR(4)
├── card_exp_month            INT
├── card_exp_year             INT
├── is_default                BOOLEAN
└── created_at                TIMESTAMPTZ

user_details (modified)
└── stripe_customer_id        VARCHAR(255) UNIQUE             [NEW]
```

---

## Index Summary

| Table | Index | Columns | Type |
|-------|-------|---------|------|
| orders | `idx_orders_user_id` | `user_id` | B-tree |
| orders | `idx_orders_user_id_status` | `user_id, status` | B-tree (composite) |
| orders | `idx_orders_user_id_session_id` | `user_id, session_id` | B-tree (composite) |
| orders | `idx_orders_idempotency_key` | `idempotency_key` | Unique partial (WHERE NOT NULL) |
| orders | `idx_orders_stripe_payment_intent_id` | `stripe_payment_intent_id` | Partial (WHERE NOT NULL) |
| order_item | `idx_order_item_order_id` | `order_id` | B-tree |
| order_status_history | `idx_order_status_history_order_id` | `order_id` | B-tree |
| order_status_history | `idx_order_status_history_changed_at` | `order_id, changed_at` | B-tree (composite) |
| user_payment_method | `idx_user_payment_method_user_id` | `user_id` | B-tree |
| user_payment_method | `idx_user_payment_method_default` | `user_id` | Unique partial (WHERE is_default) |
| user_details | `idx_user_details_stripe_customer_id` | `stripe_customer_id` | Unique partial (WHERE NOT NULL) |

-- Add order lifecycle columns

ALTER TABLE public.orders
    ADD COLUMN version INT NOT NULL DEFAULT 0;

ALTER TABLE public.orders
    ADD COLUMN idempotency_key VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_idempotency_key
    ON public.orders (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE public.orders
    ADD COLUMN cancellation_deadline TIMESTAMPTZ;

ALTER TABLE public.orders
    ADD COLUMN stripe_payment_intent_id VARCHAR(255);

ALTER TABLE public.orders
    ADD COLUMN refund_reason VARCHAR(500);

ALTER TABLE public.orders
    ADD COLUMN refunded_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_orders_stripe_payment_intent_id
    ON public.orders (stripe_payment_intent_id)
    WHERE stripe_payment_intent_id IS NOT NULL;

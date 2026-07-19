-- Scope order idempotency to a user. Two different users may send the same key.

DROP INDEX IF EXISTS public.idx_orders_idempotency_key;

CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_user_idempotency_key
    ON public.orders (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

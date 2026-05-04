-- Payment entity for Stripe integration (test mode only — no real money)

CREATE TABLE IF NOT EXISTS public.payments (
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
    updated_at                  TIMESTAMPTZ,
    created_by                  UUID,
    updated_by                  UUID
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

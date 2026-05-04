-- Stripe webhook event deduplication table

CREATE TABLE IF NOT EXISTS public.stripe_webhook_events (
    stripe_event_id  VARCHAR(255) PRIMARY KEY,
    event_type       VARCHAR(100) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    received_at      TIMESTAMPTZ  NOT NULL DEFAULT current_timestamp,
    processed_at     TIMESTAMPTZ,
    failure_reason   TEXT
);

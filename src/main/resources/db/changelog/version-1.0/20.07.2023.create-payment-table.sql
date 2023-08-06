CREATE TABLE IF NOT EXISTS payment
(
    payment_id        BIGSERIAL PRIMARY KEY,
    payment_intent_id BIGINT NOT NULL UNIQUE,
    shopping_session_id UUID NOT NULL UNIQUE,
    currency          VARCHAR(3) NOT NULL,
    items_total_price DECIMAL    NOT NULL CHECK (items_total_price > 0),
    status            VARCHAR(32),
    description       TEXT
);
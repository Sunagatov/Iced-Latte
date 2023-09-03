CREATE TABLE IF NOT EXISTS payment
(
    payment_id        BIGSERIAL PRIMARY KEY,
    payment_intent_id VARCHAR(64) NOT NULL,
    shopping_session_id UUID NOT NULL,
    items_total_price DECIMAL NOT NULL CHECK (items_total_price > 0),
    status            VARCHAR(32),
    description       TEXT,
    UNIQUE (payment_intent_id, shopping_session_id)
);
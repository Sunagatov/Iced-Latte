CREATE TABLE IF NOT EXISTS payment
(
    payment_id        BIGSERIAL PRIMARY KEY,
    currency          VARCHAR(3) NOT NULL,
    items_total_price DECIMAL    NOT NULL CHECK (items_total_price > 0),
    status            VARCHAR(32),
    description       TEXT

);
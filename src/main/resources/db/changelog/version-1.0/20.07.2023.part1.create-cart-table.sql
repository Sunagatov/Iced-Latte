CREATE TABLE IF NOT EXISTS cart
(
    id                 UUID        PRIMARY KEY,
    user_id            UUID        NOT NULL,
    items_quantity     INT         NOT NULL CHECK (items_quantity >= 0),
    products_quantity  INT         NOT NULL CHECK (products_quantity >= 0),
    total_price        DECIMAL     NOT NULL CHECK (total_price > 0),
    created_at         TIMESTAMPTZ,
    closed_at          TIMESTAMPTZ
);
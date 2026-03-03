CREATE TABLE IF NOT EXISTS public.shopping_cart
(
    id                 UUID        PRIMARY KEY,
    user_id            UUID        NOT NULL,
    items_quantity     INT         NOT NULL CHECK (items_quantity >= 0),
    products_quantity  INT         NOT NULL CHECK (products_quantity >= 0),
    created_at         TIMESTAMPTZ NOT NULL CHECK (created_at < closed_at),
    closed_at          TIMESTAMPTZ
);
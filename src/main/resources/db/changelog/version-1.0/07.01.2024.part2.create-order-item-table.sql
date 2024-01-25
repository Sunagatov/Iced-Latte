CREATE TABLE IF NOT EXISTS order_item
(
    id                UUID PRIMARY KEY,
    order_id          UUID NOT NULL,
    product_id        UUID NOT NULL,
    products_quantity INT  NOT NULL CHECK (products_quantity >= 0)
)
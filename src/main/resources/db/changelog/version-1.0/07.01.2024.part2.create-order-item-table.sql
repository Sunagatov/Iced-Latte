CREATE TABLE IF NOT EXISTS public.order_item
(
    id                UUID PRIMARY KEY,
    order_id          UUID NOT NULL,
    product_id        UUID NOT NULL,
    product_price     DECIMAL     NOT NULL CHECK (product_price > 0),
    product_name           VARCHAR(64) NOT NULL,
    products_quantity INT  NOT NULL CHECK (products_quantity >= 0),

    CONSTRAINT fk_order
        FOREIGN KEY (order_id)
            REFERENCES orders (id)
            ON DELETE CASCADE
)
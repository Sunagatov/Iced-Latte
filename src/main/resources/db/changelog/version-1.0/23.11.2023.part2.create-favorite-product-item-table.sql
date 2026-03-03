CREATE TABLE IF NOT EXISTS public.favorite_item
(
    id UUID PRIMARY KEY,
    favorite_id UUID NOT NULL,
    product_id UUID NOT NULL,
    version INT,

    CONSTRAINT fk_favorite
        FOREIGN KEY (favorite_id)
            REFERENCES favorite_list (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_product
        FOREIGN KEY (product_id)
            REFERENCES product (id)
            ON DELETE CASCADE
);

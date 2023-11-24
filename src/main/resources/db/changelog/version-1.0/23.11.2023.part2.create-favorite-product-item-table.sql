CREATE TABLE IF NOT EXISTS favorite_item
(
    id UUID PRIMARY KEY,
    favorite_id UUID NOT NULL,
    product_id UUID NOT NULL,
    version INT,

    CONSTRAINT fk_favorite
        FOREIGN KEY (favorite_id)
            REFERENCES favorite (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_product
        FOREIGN KEY (product_id)
            REFERENCES product (id)
            ON DELETE CASCADE
);

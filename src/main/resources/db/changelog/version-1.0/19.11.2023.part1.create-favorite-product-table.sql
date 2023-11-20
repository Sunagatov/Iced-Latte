CREATE TABLE IF NOT EXISTS favorite_product
(
    user_id    UUID NOT NULL,
    product_id UUID NOT NULL,

    PRIMARY KEY (user_id, product_id),

    CONSTRAINT fk_favorite_user
        FOREIGN KEY (user_id)
            REFERENCES user_details (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_favorite_product
        FOREIGN KEY (product_id)
            REFERENCES product (id)
            ON DELETE CASCADE
);

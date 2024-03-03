CREATE TABLE IF NOT EXISTS product_rating
(
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    product_id  UUID NOT NULL,
    rating_value INT  NOT NULL CHECK (rating_value > 0 AND rating_value < 6),

    CONSTRAINT fk_user_rating
        FOREIGN KEY (user_id)
            REFERENCES user_details (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_product_rating
        FOREIGN KEY (product_id)
            REFERENCES product (id)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS product_rating
(
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    product_id  UUID NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT current_timestamp,
    productRating        INT  NOT NULL CHECK (productRating > 0 AND mark < 6),

    CONSTRAINT fk_user_rating
        FOREIGN KEY (user_id)
            REFERENCES user_details (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_product_rating
        FOREIGN KEY (product_id)
            REFERENCES product (id)
            ON DELETE CASCADE
);

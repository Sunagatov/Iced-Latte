CREATE TABLE IF NOT EXISTS product_rating
(
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    product_id  UUID NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT current_timestamp,
    rating      INT  NOT NULL CHECK (rating > 0 AND rating < 6),

    CONSTRAINT fk_user
        FOREIGN KEY (user_id)
            REFERENCES user_details (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_product
        FOREIGN KEY (product_id)
            REFERENCES product (id)
            ON DELETE CASCADE
);

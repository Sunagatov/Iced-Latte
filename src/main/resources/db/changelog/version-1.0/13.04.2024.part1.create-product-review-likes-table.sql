CREATE TABLE IF NOT EXISTS public.product_reviews_likes
(
    id         UUID PRIMARY KEY,
    review_id  UUID    NOT NULL,
    product_id UUID    NOT NULL,
    user_id    UUID    NOT NULL,
    is_like    BOOLEAN NOT NULL,

    CONSTRAINT fk_review
        FOREIGN KEY (review_id)
            REFERENCES product_reviews (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_product
        FOREIGN KEY (product_id)
            REFERENCES product (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_user
        FOREIGN KEY (user_id)
            REFERENCES user_details (id)
            ON DELETE CASCADE
)

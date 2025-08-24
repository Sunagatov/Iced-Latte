CREATE TABLE IF NOT EXISTS public.product_reviews
(
    id             UUID PRIMARY KEY,
    product_id     UUID          NOT NULL,
    user_id        UUID          NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    text           VARCHAR(1500) NOT NULL,
    rating         INT           NOT NULL CHECK (rating > 0 AND rating < 6),
    likes_count    INT           NOT NULL CHECK (likes_count >= 0),
    dislikes_count INT           NOT NULL CHECK (dislikes_count >= 0),

    CONSTRAINT fk_user
        FOREIGN KEY (user_id)
            REFERENCES public.user_details (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_product
        FOREIGN KEY (product_id)
            REFERENCES public.product (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_product_reviews_user_product
        UNIQUE (user_id, product_id)
);

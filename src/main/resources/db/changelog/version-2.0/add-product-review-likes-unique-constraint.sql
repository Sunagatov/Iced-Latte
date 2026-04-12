ALTER TABLE public.product_reviews_likes
    ADD CONSTRAINT uq_product_reviews_likes_user_review UNIQUE (user_id, review_id);

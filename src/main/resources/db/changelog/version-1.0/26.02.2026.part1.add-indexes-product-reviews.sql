CREATE INDEX IF NOT EXISTS idx_product_reviews_product_id
    ON public.product_reviews (product_id);

CREATE INDEX IF NOT EXISTS idx_product_reviews_product_id_rating
    ON public.product_reviews (product_id, rating);

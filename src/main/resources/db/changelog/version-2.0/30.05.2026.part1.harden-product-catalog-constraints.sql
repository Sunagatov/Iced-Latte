--liquibase formatted sql

--changeset zufar:harden-product-catalog-constraints
UPDATE public.product
   SET description = ''
 WHERE description IS NULL;

UPDATE public.product
   SET average_rating = 0
 WHERE average_rating IS NULL;

UPDATE public.product
   SET reviews_count = 0
 WHERE reviews_count IS NULL;

ALTER TABLE public.product
    ALTER COLUMN description SET NOT NULL,
    ALTER COLUMN average_rating SET NOT NULL,
    ALTER COLUMN reviews_count SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_product_image_product_position_id
    ON public.product_image (product_id, position, id);

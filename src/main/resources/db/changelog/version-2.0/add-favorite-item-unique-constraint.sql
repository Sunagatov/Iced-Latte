-- Remove duplicate (favorite_id, product_id) rows, keeping one row per pair.
-- MIN(uuid) does not exist in PostgreSQL; use DISTINCT ON to pick one row per pair instead.
DELETE FROM public.favorite_item
WHERE id NOT IN (
    SELECT DISTINCT ON (favorite_id, product_id) id
    FROM public.favorite_item
    ORDER BY favorite_id, product_id, id
);

ALTER TABLE public.favorite_item
    ADD CONSTRAINT uq_favorite_item_list_product UNIQUE (favorite_id, product_id);

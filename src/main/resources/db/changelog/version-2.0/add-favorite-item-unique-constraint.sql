-- Remove duplicate (favorite_id, product_id) rows, keeping the one with the smallest id.
DELETE FROM public.favorite_item
WHERE id NOT IN (
    SELECT MIN(id)
    FROM public.favorite_item
    GROUP BY favorite_id, product_id
);

ALTER TABLE public.favorite_item
    ADD CONSTRAINT uq_favorite_item_list_product UNIQUE (favorite_id, product_id);

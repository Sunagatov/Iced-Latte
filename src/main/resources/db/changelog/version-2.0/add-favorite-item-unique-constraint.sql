ALTER TABLE public.favorite_item
    ADD CONSTRAINT uq_favorite_item_list_product UNIQUE (favorite_id, product_id);

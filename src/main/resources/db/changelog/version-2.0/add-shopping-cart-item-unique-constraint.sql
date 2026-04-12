ALTER TABLE public.shopping_cart_item
    ADD CONSTRAINT uq_shopping_cart_item_cart_product UNIQUE (shopping_cart_id, product_id);

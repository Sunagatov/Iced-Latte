ALTER TABLE public.shopping_cart_item
    ADD CONSTRAINT chk_shopping_cart_item_quantity_range
        CHECK (products_quantity BETWEEN 1 AND 99);

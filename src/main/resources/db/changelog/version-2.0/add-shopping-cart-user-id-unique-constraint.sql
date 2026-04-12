ALTER TABLE public.shopping_cart
    ADD CONSTRAINT uq_shopping_cart_user_id UNIQUE (user_id);

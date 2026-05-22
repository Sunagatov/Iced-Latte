DELETE FROM public.shopping_cart_item sci
WHERE NOT EXISTS (
    SELECT 1
    FROM public.shopping_cart sc
    WHERE sc.id = sci.shopping_cart_id
);

DELETE FROM public.shopping_cart_item sci
WHERE NOT EXISTS (
    SELECT 1
    FROM public.product p
    WHERE p.id = sci.product_id
);

ALTER TABLE public.shopping_cart_item
    ADD CONSTRAINT fk_shopping_cart_item_cart
        FOREIGN KEY (shopping_cart_id)
            REFERENCES public.shopping_cart (id)
            ON DELETE CASCADE;

ALTER TABLE public.shopping_cart_item
    ADD CONSTRAINT fk_shopping_cart_item_product
        FOREIGN KEY (product_id)
            REFERENCES public.product (id)
            ON DELETE CASCADE;

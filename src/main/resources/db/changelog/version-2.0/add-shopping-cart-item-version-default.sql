UPDATE public.shopping_cart_item
SET version = 0
WHERE version IS NULL;

ALTER TABLE public.shopping_cart_item
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL;

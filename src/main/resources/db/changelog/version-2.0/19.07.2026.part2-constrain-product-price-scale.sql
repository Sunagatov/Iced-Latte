-- Align the database price type with ProductInfo.price precision/scale.

ALTER TABLE public.product
    ALTER COLUMN price TYPE NUMERIC(19, 2)
    USING ROUND(price, 2);

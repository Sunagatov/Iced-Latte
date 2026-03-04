--liquibase formatted sql
--changeset liquibase:fix-product-discount-check runInTransaction:false

ALTER TABLE public.product
    DROP CONSTRAINT IF EXISTS product_discount_check,
    ADD CONSTRAINT product_discount_check CHECK (discount >= 0);

-- items_quantity and products_quantity were denormalized counters that were kept
-- manually in sync with the shopping_cart_item rows. They are now derived via
-- @Formula (SQL subquery) on the ShoppingCart entity, so the stored columns are
-- no longer written by Hibernate and can be removed.
ALTER TABLE public.shopping_cart
    DROP COLUMN IF EXISTS items_quantity,
    DROP COLUMN IF EXISTS products_quantity;

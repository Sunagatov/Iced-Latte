-- Enforce at most one default delivery address per user at the database level.
-- A partial unique index covers only rows where is_default = true, so non-default
-- addresses are unaffected.
CREATE UNIQUE INDEX idx_delivery_address_one_default_per_user
    ON public.delivery_address (user_id)
    WHERE is_default = TRUE;

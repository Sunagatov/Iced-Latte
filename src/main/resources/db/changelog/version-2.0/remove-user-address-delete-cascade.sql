ALTER TABLE public.user_details
    DROP CONSTRAINT IF EXISTS fk_address;

ALTER TABLE public.user_details
    ADD CONSTRAINT fk_user_details_address
        FOREIGN KEY (address_id)
            REFERENCES public.address (id);

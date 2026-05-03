-- Add indexes and FK constraint for orders module

CREATE INDEX IF NOT EXISTS idx_orders_user_id
    ON public.orders (user_id);

CREATE INDEX IF NOT EXISTS idx_order_item_order_id
    ON public.order_item (order_id);

ALTER TABLE public.orders
    ADD CONSTRAINT fk_orders_user
    FOREIGN KEY (user_id) REFERENCES public.user_details(id);

CREATE INDEX IF NOT EXISTS idx_orders_user_id_status
    ON public.orders (user_id, status);

CREATE INDEX IF NOT EXISTS idx_orders_user_id_session_id
    ON public.orders (user_id, session_id);

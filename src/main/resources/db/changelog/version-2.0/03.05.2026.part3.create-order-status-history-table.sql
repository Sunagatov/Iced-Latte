-- Order status history audit trail

CREATE TABLE IF NOT EXISTS public.order_status_history (
    id          UUID PRIMARY KEY,
    order_id    UUID NOT NULL,
    old_status  VARCHAR(55),
    new_status  VARCHAR(55) NOT NULL,
    changed_by  UUID,
    reason      VARCHAR(500),
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,

    CONSTRAINT fk_order_status_history_order
        FOREIGN KEY (order_id)
        REFERENCES public.orders(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_order_status_history_order_id
    ON public.order_status_history (order_id);

CREATE INDEX IF NOT EXISTS idx_order_status_history_changed_at
    ON public.order_status_history (order_id, changed_at);

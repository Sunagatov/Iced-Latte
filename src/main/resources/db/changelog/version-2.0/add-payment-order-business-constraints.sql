-- Harden payment/order business invariants at the database boundary.

ALTER TABLE public.payments
    ADD CONSTRAINT chk_payments_amount_minor_positive
        CHECK (amount_minor > 0);

ALTER TABLE public.payments
    ADD CONSTRAINT chk_payments_currency_lowercase_iso_4217
        CHECK (currency ~ '^[a-z]{3}$');

ALTER TABLE public.payments
    ADD CONSTRAINT chk_payments_provider_known
        CHECK (provider IN ('STRIPE'));

ALTER TABLE public.payments
    ADD CONSTRAINT chk_payments_status_known
        CHECK (status IN (
            'CREATED',
            'STRIPE_SESSION_CREATED',
            'AWAITING_ASYNC_CONFIRMATION',
            'PAID',
            'FAILED',
            'EXPIRED',
            'REFUNDED',
            'RECONCILIATION_FAILED'
        ));

ALTER TABLE public.stripe_webhook_events
    ADD CONSTRAINT chk_stripe_webhook_events_status_known
        CHECK (status IN (
            'PROCESSING',
            'PROCESSED',
            'RETRYABLE_FAILED'
        ));

ALTER TABLE public.orders
    ADD CONSTRAINT chk_orders_items_quantity_positive
        CHECK (items_quantity > 0);

ALTER TABLE public.orders
    ADD CONSTRAINT chk_orders_status_known
        CHECK (status IN (
            'PENDING_PAYMENT',
            'CREATED',
            'PAID',
            'DELIVERY',
            'SHIPPED',
            'FINISHED',
            'DELIVERED',
            'CANCELLED',
            'REFUND_REQUESTED',
            'REFUNDED',
            'PAYMENT_FAILED',
            'PAYMENT_EXPIRED'
        ));

ALTER TABLE public.order_item
    ADD CONSTRAINT chk_order_item_products_quantity_positive
        CHECK (products_quantity > 0);

ALTER TABLE public.order_status_history
    ADD CONSTRAINT chk_order_status_history_old_status_known
        CHECK (old_status IS NULL OR old_status IN (
            'PENDING_PAYMENT',
            'CREATED',
            'PAID',
            'DELIVERY',
            'SHIPPED',
            'FINISHED',
            'DELIVERED',
            'CANCELLED',
            'REFUND_REQUESTED',
            'REFUNDED',
            'PAYMENT_FAILED',
            'PAYMENT_EXPIRED'
        ));

ALTER TABLE public.order_status_history
    ADD CONSTRAINT chk_order_status_history_new_status_known
        CHECK (new_status IN (
            'PENDING_PAYMENT',
            'CREATED',
            'PAID',
            'DELIVERY',
            'SHIPPED',
            'FINISHED',
            'DELIVERED',
            'CANCELLED',
            'REFUND_REQUESTED',
            'REFUNDED',
            'PAYMENT_FAILED',
            'PAYMENT_EXPIRED'
        ));

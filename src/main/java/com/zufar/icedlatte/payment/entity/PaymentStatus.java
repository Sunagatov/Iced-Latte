package com.zufar.icedlatte.payment.entity;

/**
 * Tracks the payment lifecycle within Iced Latte.
 * Stripe is the only provider (test mode / sandbox only — no real money).
 */
public enum PaymentStatus {
    CREATED,
    STRIPE_SESSION_CREATED,
    AWAITING_ASYNC_CONFIRMATION,
    PAID,
    FAILED,
    EXPIRED,
    REFUNDED,
    RECONCILIATION_FAILED
}

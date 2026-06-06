package com.zufar.icedlatte.payment.service.webhook;

enum StripeWebhookEventType {
    CHECKOUT_SESSION_COMPLETED("checkout.session.completed"),
    CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED("checkout.session.async_payment_succeeded"),
    CHECKOUT_SESSION_EXPIRED("checkout.session.expired"),
    CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED("checkout.session.async_payment_failed"),
    CHARGE_REFUNDED("charge.refunded");

    private final String value;

    StripeWebhookEventType(String value) {
        this.value = value;
    }

    boolean matches(String eventType) {
        return value.equals(eventType);
    }
}

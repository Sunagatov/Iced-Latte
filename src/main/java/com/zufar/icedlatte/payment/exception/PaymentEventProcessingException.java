package com.zufar.icedlatte.payment.exception;

public final class PaymentEventProcessingException extends PaymentException {

    public PaymentEventProcessingException() {
        super("Stripe webhook signature verification failed.");
    }
}

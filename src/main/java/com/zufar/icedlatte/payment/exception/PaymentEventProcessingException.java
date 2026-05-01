package com.zufar.icedlatte.payment.exception;

public class PaymentEventProcessingException extends RuntimeException {

    public PaymentEventProcessingException() {
        super("Stripe webhook signature verification failed.");
    }
}

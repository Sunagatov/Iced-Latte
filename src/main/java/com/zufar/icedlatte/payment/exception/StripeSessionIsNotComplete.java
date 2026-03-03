package com.zufar.icedlatte.payment.exception;

public class StripeSessionIsNotComplete extends RuntimeException {

    public StripeSessionIsNotComplete(final String sessionId, final String status) {
        super(String.format("Error processing Stripe session with id = '%s' and status = '%s'", sessionId, status));
    }
}

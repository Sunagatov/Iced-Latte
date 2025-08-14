package com.zufar.icedlatte.payment.exception;

public class StripeSessionRetrievalException extends RuntimeException {

    public StripeSessionRetrievalException(final String message,
                                           final String sessionId) {
        super(String.format("Error retrieving Stripe session with id = '%s'. Error message = '%s'", sessionId, message));
    }

    public StripeSessionRetrievalException(final String message,
                                           final String sessionId,
                                           final Throwable cause) {
        super(String.format("Error retrieving Stripe session with id = '%s'. Error message = '%s'", sessionId, message), cause);
    }
}

package com.zufar.icedlatte.payment.exception;

public class StripeSessionCreationException extends RuntimeException {

    public StripeSessionCreationException(final String message, final Throwable cause) {
        super(String.format("Error creating Stripe session. Error message = '%s'", message), cause);
    }
}

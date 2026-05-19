package com.zufar.icedlatte.payment.exception;

public final class StripeSessionCreationException extends PaymentException {

    public StripeSessionCreationException(final String message, final Throwable cause) {
        super(String.format("Error creating Stripe session. Error message = '%s'", message), cause);
    }
}

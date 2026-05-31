package com.zufar.icedlatte.payment.exception;

public final class StripeSessionException extends PaymentException {

    public StripeSessionException(final String message, final Throwable cause) {
        super(String.format("Stripe session operation failed. Error message = '%s'", message), cause);
    }
}

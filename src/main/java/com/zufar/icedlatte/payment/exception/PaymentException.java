package com.zufar.icedlatte.payment.exception;

/**
 * Sealed base for all payment-related exceptions.
 * Enables exhaustive pattern matching in switch expressions (Java 25).
 */
public abstract sealed class PaymentException extends RuntimeException
        permits PaymentEventProcessingException, StripeSessionCreationException {

    protected PaymentException(String message) {
        super(message);
    }

    protected PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}

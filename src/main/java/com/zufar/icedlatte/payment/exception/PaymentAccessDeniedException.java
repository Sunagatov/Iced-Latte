package com.zufar.icedlatte.payment.exception;

public final class PaymentAccessDeniedException extends PaymentException {

    public PaymentAccessDeniedException() {
        super("Access denied.");
    }
}

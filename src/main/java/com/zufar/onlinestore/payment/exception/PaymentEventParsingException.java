package com.zufar.onlinestore.payment.exception;

public class PaymentEventParsingException extends RuntimeException {

    public PaymentEventParsingException() {
        super("Payment event can't be parsing.");
    }
}

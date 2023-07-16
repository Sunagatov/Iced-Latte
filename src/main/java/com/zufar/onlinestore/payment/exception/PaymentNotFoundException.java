package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

@Getter
public class PaymentNotFoundException extends RuntimeException {

    private final String paymentId;

    public PaymentNotFoundException(String paymentId) {
        super(String.format("Payment with id %s not found.", paymentId));
        this.paymentId = paymentId;
    }
}

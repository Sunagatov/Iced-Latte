package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

@Getter
public class PaymentNotFoundException extends RuntimeException {

    private final Long paymentId;

    public PaymentNotFoundException(Long paymentId) {
        super(String.format("Payment with id %s not found.", paymentId));
        this.paymentId = paymentId;
    }
}

package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

@Getter
public class PaymentProcessingException extends RuntimeException {

    private final String paymentId;

    public PaymentProcessingException(String paymentMethodId) {
        super(String.format("Cannot process payment with ID: %s ", paymentMethodId));
        this.paymentId = paymentMethodId;
    }
}

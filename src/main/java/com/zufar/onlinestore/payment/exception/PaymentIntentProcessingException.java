package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

@Getter
public class PaymentIntentProcessingException extends RuntimeException{

    private final String paymentId;

    public PaymentIntentProcessingException(String paymentMethodId) {
        super(String.format("Cannot process payment intent with payment method Id: %s ", paymentMethodId));
        this.paymentId = paymentMethodId;
    }
}

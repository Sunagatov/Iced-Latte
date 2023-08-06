package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

@Getter
public class PaymentMethodProcessingException extends RuntimeException {

    private final String paymentMethodType;

    public PaymentMethodProcessingException(String paymentMethodType) {
        super(String.format("Payment method with the type = '%s' cannot be processed.", paymentMethodType));
        this.paymentMethodType = paymentMethodType;
    }
}

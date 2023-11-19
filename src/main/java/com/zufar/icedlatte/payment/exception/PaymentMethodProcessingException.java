package com.zufar.icedlatte.payment.exception;

import lombok.Getter;

@Getter
public class PaymentMethodProcessingException extends RuntimeException {

    private final String paymentMethodType;

    public PaymentMethodProcessingException(String paymentMethodType) {
        super(String.format("Cannot process payment method with type: %s.", paymentMethodType));
        this.paymentMethodType = paymentMethodType;
    }
}

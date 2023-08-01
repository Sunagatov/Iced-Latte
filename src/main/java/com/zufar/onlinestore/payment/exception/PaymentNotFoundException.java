package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

@Getter
public class PaymentNotFoundException extends RuntimeException {

    private final Long paymentId;

    public PaymentNotFoundException(final Long paymentId) {
        super(String.format("The payment with paymentId = %s is not found", paymentId));
        this.paymentId = paymentId;
    }
}

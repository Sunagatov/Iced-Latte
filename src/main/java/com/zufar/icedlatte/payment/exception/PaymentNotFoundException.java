package com.zufar.icedlatte.payment.exception;

import lombok.Getter;

@Getter
public class PaymentNotFoundException extends RuntimeException {

    private final Long paymentId;

    public PaymentNotFoundException(final Long paymentId) {
        super(String.format("The payment with paymentId = %d is not found", paymentId));
        this.paymentId = paymentId;
    }
}

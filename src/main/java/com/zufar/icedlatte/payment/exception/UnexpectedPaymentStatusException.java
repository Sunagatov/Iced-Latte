package com.zufar.icedlatte.payment.exception;

import com.zufar.icedlatte.payment.enums.PaymentStatus;
import lombok.Getter;

@Getter
public class UnexpectedPaymentStatusException extends RuntimeException {

    private final PaymentStatus paymentStatus;

    public UnexpectedPaymentStatusException(final PaymentStatus paymentStatus) {
        super(String.format("Payment status = %s is unexpected.", paymentStatus));
        this.paymentStatus = paymentStatus;
    }
}

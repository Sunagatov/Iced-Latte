package com.zufar.onlinestore.payment.exception;

import com.zufar.onlinestore.payment.enums.PaymentConstants;
import lombok.Getter;

@Getter
public class UnexpectedPaymentStatusException extends RuntimeException {

    private final PaymentConstants paymentConstants;

    public UnexpectedPaymentStatusException(final PaymentConstants paymentConstants) {
        super(String.format("Payment status: %s is unexpected.", paymentConstants));
        this.paymentConstants = paymentConstants;
    }
}

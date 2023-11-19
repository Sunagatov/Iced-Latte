package com.zufar.icedlatte.payment.exception;

import lombok.Getter;

@Getter
public class PaymentMethodNotFoundException extends RuntimeException {

    private final String stripeCustomerId;

    public PaymentMethodNotFoundException(String stripeCustomerId) {
        super(String.format("The payment associated with customer is not found, stripeCustomerId = %s.", stripeCustomerId));
        this.stripeCustomerId = stripeCustomerId;
    }
}

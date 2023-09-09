package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

@Getter
public class PaymentMethodRetrievingException extends RuntimeException {

    private final String stripeCustomerId;

    public PaymentMethodRetrievingException(String stripeCustomerId) {
        super(String.format("Cannot retrieve payment method associated with stripe customer, " +
                "stripeCustomerId = %s.", stripeCustomerId));
        this.stripeCustomerId = stripeCustomerId;
    }
}

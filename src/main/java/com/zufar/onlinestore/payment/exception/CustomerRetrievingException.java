package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

@Getter
public class CustomerRetrievingException extends RuntimeException {

    private final String stripeCustomerId;

    public CustomerRetrievingException(String stripeCustomerId) {
        super(String.format("Cannot retrieve stripe customer by stripeCustomerId = %s.", stripeCustomerId));
        this.stripeCustomerId = stripeCustomerId;
    }
}

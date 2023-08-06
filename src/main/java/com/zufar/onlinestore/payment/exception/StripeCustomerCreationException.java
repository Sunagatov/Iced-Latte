package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

@Getter
public class StripeCustomerCreationException extends RuntimeException {

    private final String customerEmail;

    public StripeCustomerCreationException(String customerEmail) {
        super(String.format("Cannot create customer with email = %s.", customerEmail));
        this.customerEmail = customerEmail;
    }
}

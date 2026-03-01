package com.zufar.icedlatte.payment.exception;

import lombok.Getter;

@Getter
public class PaymentEventProcessingException extends RuntimeException {

    private final String stripeSignature;

    public PaymentEventProcessingException(final String stripeSignature) {
        super(String.format("Payment event with the Stripe signature = '%s' cannot be processed.", stripeSignature));
        this.stripeSignature = stripeSignature;
    }
}

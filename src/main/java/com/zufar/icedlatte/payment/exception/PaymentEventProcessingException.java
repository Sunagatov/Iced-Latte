package com.zufar.icedlatte.payment.exception;

import lombok.Getter;

@Getter
public class PaymentEventProcessingException extends RuntimeException {

    private final String stripeSignature;

    public PaymentEventProcessingException(String stripeSignature) {
        super(String.format("Payment event with the stripe signature = '%s' cannot be processed.", stripeSignature));
        this.stripeSignature = stripeSignature;
    }
}

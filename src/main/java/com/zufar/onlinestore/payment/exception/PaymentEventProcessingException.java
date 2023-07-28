package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

@Getter
public class PaymentEventProcessingException extends RuntimeException {

    private final String stripeSignature;

    public PaymentEventProcessingException(String stripeSignatureHeader) {
        super(String.format("Payment event with stripe signature %s, can't be processed.", stripeSignatureHeader));
        this.stripeSignature = stripeSignatureHeader;
    }
}

package com.zufar.icedlatte.payment.exception;

import lombok.Getter;

@Getter
public class StripeSessionIsNotComplete extends RuntimeException {

    private final String status;

    public StripeSessionIsNotComplete(final String sessionId, final String status) {
        super(String.format("Error processing Stripe session with id = '%s' and status = '%s'", sessionId, status));
        this.status = status;
    }
}

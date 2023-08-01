package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

@Getter
public class PaymentEventParsingException extends RuntimeException {

    private final String eventType;

    public PaymentEventParsingException(String eventType) {
        super(String.format("Payment event with '%s' cannot be parsed.", eventType));
        this.eventType = eventType;
    }
}

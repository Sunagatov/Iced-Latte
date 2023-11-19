package com.zufar.icedlatte.payment.exception;

import lombok.Getter;

@Getter
public class CardTokenCreationException extends RuntimeException {

    private final String cardNumber;

    public CardTokenCreationException(String cardNumber) {
        super(String.format("Card token with cardNumber = %s cannot create", cardNumber));
        this.cardNumber = cardNumber;
    }
}

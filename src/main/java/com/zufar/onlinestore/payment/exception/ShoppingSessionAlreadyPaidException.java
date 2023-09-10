package com.zufar.onlinestore.payment.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShoppingSessionAlreadyPaidException extends RuntimeException {

    private final UUID shoppingSessionId;

    public ShoppingSessionAlreadyPaidException(UUID shoppingSessionId) {
        super(String.format("Shopping session with shoppingSessionId = %s is already paid", shoppingSessionId));
        this.shoppingSessionId = shoppingSessionId;
    }
}

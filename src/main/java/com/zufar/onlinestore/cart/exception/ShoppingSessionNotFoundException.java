package com.zufar.onlinestore.cart.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShoppingSessionNotFoundException extends RuntimeException {

    private final UUID shoppingSessionId;

    public ShoppingSessionNotFoundException(final UUID shoppingSessionId) {
        super(String.format("The shopping session with shoppingSessionId = %s is not found.", shoppingSessionId));
        this.shoppingSessionId = shoppingSessionId;
    }
}

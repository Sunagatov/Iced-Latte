package com.zufar.onlinestore.cart.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShoppingSessionItemNotFoundException extends RuntimeException {

    private final UUID shoppingSessionId;
    private final UUID shoppingSessionItemId;

    public ShoppingSessionItemNotFoundException(final UUID shoppingSessionId, final UUID shoppingSessionItemId) {
        super(String.format("The shopping session item with shoppingSessionItemId = %s and shoppingSessionId = %s  is not found.",
                shoppingSessionItemId, shoppingSessionId));
        this.shoppingSessionId = shoppingSessionId;
        this.shoppingSessionItemId = shoppingSessionItemId;
    }
}

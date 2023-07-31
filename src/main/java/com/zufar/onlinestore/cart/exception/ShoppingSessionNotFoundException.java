package com.zufar.onlinestore.cart.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShoppingSessionNotFoundException extends RuntimeException {

    private final UUID userId;
    private final UUID shoppingSessionId;
    private final UUID shoppingSessionItemId;

    //TODO I am going to remove this constructor in the another PR
    public ShoppingSessionNotFoundException(final UUID shoppingSessionId, final UUID shoppingSessionItemId) {
        super(String.format("The shopping session with shoppingSessionItemId = %s and shoppingSessionId = %s  is not found.",
                shoppingSessionItemId, shoppingSessionId));
        this.shoppingSessionId = shoppingSessionId;
        this.shoppingSessionItemId = shoppingSessionItemId;
        this.userId = null;
    }

    public ShoppingSessionNotFoundException(final UUID userId) {
        super(String.format("The shopping session for the user with id = %s is not found.", userId));
        this.userId = userId;
        this.shoppingSessionId = null;
        this.shoppingSessionItemId = null;
    }
}

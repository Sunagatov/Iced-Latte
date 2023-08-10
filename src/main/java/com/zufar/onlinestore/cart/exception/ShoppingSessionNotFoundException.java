package com.zufar.onlinestore.cart.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShoppingSessionNotFoundException extends RuntimeException {

    private final UUID userId;

    public ShoppingSessionNotFoundException(final UUID userId) {
        super(String.format("The shopping session for the user with id = %s is not found.", userId));
        this.userId = userId;
    }
}

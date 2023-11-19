package com.zufar.icedlatte.cart.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShoppingCartNotFoundException extends RuntimeException {

    private final UUID userId;

    public ShoppingCartNotFoundException(final UUID userId) {
        super(String.format("The shopping cart for the user with id = %s is not found.", userId));
        this.userId = userId;
    }
}

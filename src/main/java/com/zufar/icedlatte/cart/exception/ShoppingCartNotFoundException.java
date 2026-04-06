package com.zufar.icedlatte.cart.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShoppingCartNotFoundException extends RuntimeException {

    private final UUID userId;

    public ShoppingCartNotFoundException(final UUID userId) {
        String format = "The shopping cart for the user with id = %s is not found.";

        super(String.format(format, userId));
        this.userId = userId;
    }
}

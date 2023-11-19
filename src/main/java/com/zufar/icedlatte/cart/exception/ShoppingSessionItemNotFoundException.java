package com.zufar.icedlatte.cart.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShoppingSessionItemNotFoundException extends RuntimeException {

    private final UUID shoppingSessionItemId;

    public ShoppingSessionItemNotFoundException(final UUID shoppingSessionItemId) {
        super(String.format("The shopping session item with shoppingSessionItemId = %s is not found.", shoppingSessionItemId));
        this.shoppingSessionItemId = shoppingSessionItemId;
    }
}

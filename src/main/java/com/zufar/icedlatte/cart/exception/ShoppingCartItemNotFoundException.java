package com.zufar.icedlatte.cart.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShoppingCartItemNotFoundException extends RuntimeException {

    private final UUID shoppingCartItemId;

    public ShoppingCartItemNotFoundException(final UUID shoppingCartItemId) {
        String format = "The shopping cart item with shoppingCartItemId = %s is not found.";

        super(String.format(format, shoppingCartItemId));
        this.shoppingCartItemId = shoppingCartItemId;
    }
}

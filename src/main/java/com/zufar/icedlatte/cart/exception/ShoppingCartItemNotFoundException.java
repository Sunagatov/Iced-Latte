package com.zufar.icedlatte.cart.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShoppingCartItemNotFoundException extends RuntimeException {

    private final UUID shoppingCartItemId;

    public ShoppingCartItemNotFoundException(final UUID shoppingCartItemId) {
        super(String.format("The shopping cart item with shoppingCartItemId = %s is not found.", shoppingCartItemId));
        this.shoppingCartItemId = shoppingCartItemId;
    }
}

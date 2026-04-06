package com.zufar.icedlatte.cart.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class InvalidShoppingCartIdException extends RuntimeException {

    private final UUID shoppingCartId;

    public InvalidShoppingCartIdException(final UUID shoppingCartId) {
        String format = "The shopping cart id = %s is invalid in UpdateProductsQuantityInShoppingCartItemRequest.";

        super(String.format(format, shoppingCartId));
        this.shoppingCartId = shoppingCartId;
    }
}

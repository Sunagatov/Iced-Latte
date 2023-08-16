package com.zufar.onlinestore.cart.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class InvalidShoppingSessionIdException extends RuntimeException {

    private final UUID shoppingSessionId;

    public InvalidShoppingSessionIdException(final UUID shoppingSessionId) {
        super(String.format("The shopping session id = %s is invalid in UpdateProductsQuantityInShoppingSessionItemRequest.", shoppingSessionId));
        this.shoppingSessionId = shoppingSessionId;
    }
}

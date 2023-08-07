package com.zufar.onlinestore.cart.exception;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class ShoppingSessionItemNotFoundException extends RuntimeException {

    private final UUID shoppingSessionId;
    private final UUID shoppingSessionItemId;
    private final List<UUID> shoppingSessionItemIds;

    public ShoppingSessionItemNotFoundException(final UUID shoppingSessionId, final UUID shoppingSessionItemId) {
        super(String.format("The shopping session item with shoppingSessionItemId = %s and shoppingSessionId = %s  is not found.",
                shoppingSessionItemId, shoppingSessionId));
        this.shoppingSessionId = shoppingSessionId;
        this.shoppingSessionItemId = shoppingSessionItemId;
        this.shoppingSessionItemIds = null;
    }

    public ShoppingSessionItemNotFoundException(final List<UUID> shoppingSessionItemIds) {
        super(String.format("The list of shopping session items with ids = %s is not found.",
                shoppingSessionItemIds));
        this.shoppingSessionId = null;
        this.shoppingSessionItemId = null;
        this.shoppingSessionItemIds = shoppingSessionItemIds;
    }
}

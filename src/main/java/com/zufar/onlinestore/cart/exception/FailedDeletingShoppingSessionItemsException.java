package com.zufar.onlinestore.cart.exception;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class FailedDeletingShoppingSessionItemsException extends RuntimeException {

    private final List<UUID> itemIds;

    public FailedDeletingShoppingSessionItemsException(final List<UUID> itemIds) {
        super(String.format("Deleting the list of the shopping session items with ids = '%s' were failed.", itemIds));
        this.itemIds = itemIds;
    }
}

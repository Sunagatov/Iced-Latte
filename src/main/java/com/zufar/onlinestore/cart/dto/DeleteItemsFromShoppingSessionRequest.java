package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record DeleteItemsFromShoppingSessionRequest(

        @NotNull(message = "List of shoppingSessionItemIds is the mandatory attribute")
        List<UUID> shoppingSessionItemIds
) {
}

package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record RemoveItemFromShoppingSessionRequest(

        @NotNull(message = "List of shoppingSessionItemIds is the mandatory attribute")
        List<UUID> shoppingSessionItemId,

        @NotNull(message = "UserId is the mandatory attribute")
        UUID userId
) {
}

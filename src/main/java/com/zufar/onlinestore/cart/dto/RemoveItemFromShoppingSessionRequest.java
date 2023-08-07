package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RemoveItemFromShoppingSessionRequest(

        @NotNull(message = "ShoppingSessionItemId is the mandatory attribute")
        UUID shoppingSessionItemId,

        @NotNull(message = "UserId is the mandatory attribute")
        UUID userId
) {
}

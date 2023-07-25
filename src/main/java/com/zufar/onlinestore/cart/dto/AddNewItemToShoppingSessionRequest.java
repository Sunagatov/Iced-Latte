package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddNewItemToShoppingSessionRequest(

        @NotNull(message = "ShoppingSessionId is the mandatory attribute")
        UUID shoppingSessionId,

        @NotNull(message = "ShoppingSessionItemId is the mandatory attribute")
        UUID shoppingSessionItemId,

        @NotNull(message = "ProductId is the mandatory attribute")
        UUID productId
) {
}

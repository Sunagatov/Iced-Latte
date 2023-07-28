package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddNewProductToShoppingSessionRequest(

        @NotNull(message = "UserId is the mandatory attribute")
        UUID userId,

        @NotNull(message = "ProductId is the mandatory attribute")
        String productId
) {
}

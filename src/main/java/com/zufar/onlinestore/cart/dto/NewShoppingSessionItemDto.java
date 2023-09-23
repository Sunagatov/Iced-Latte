package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NewShoppingSessionItemDto(

        @NotNull(message = "ProductId is the mandatory attribute")
        UUID productId,

        @NotNull(message = "ProductsQuantity is the mandatory attribute")
        Integer productQuantity
) {
}

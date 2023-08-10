package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

public record AddNewItemToShoppingSessionRequest(

        @NotNull(message = "ProductId is the mandatory attribute")
        String productId
) {
}

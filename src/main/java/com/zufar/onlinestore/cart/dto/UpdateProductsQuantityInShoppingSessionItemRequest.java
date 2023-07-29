package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateProductsQuantityInShoppingSessionItemRequest(

        @NotNull(message = "UserId is the mandatory attribute")
        UUID userId,

        @NotNull(message = "ShoppingSessionItemId is the mandatory attribute")
        UUID shoppingSessionItemId,

        @NotNull(message = "Change is the mandatory attribute")
        Integer productsQuantityChange
) {
}

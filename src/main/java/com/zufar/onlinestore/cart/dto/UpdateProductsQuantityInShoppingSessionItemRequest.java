package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateProductsQuantityInShoppingSessionItemRequest(

        @NotNull(message = "ShoppingSessionId is mandatory")
        UUID shoppingSessionId,

        @NotNull(message = "ShoppingSessionItemId is mandatory")
        UUID shoppingSessionItemId,

        @NotNull(message = "Change is mandatory")
        Integer productsQuantityChange
) {
}

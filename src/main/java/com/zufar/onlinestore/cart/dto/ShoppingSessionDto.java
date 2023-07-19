package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public record ShoppingSessionDto(

        @NotNull(message = "Id is mandatory")
        UUID id,

        @NotNull(message = "UserId is mandatory")
        UUID userId,

        @NotNull(message = "Items are mandatory")
        Collection<ShoppingSessionItemDto> items,

        @NotNull(message = "ItemsQuantity is mandatory")
        Integer itemsQuantity,

        @NotNull(message = "ProductsQuantity is mandatory")
        Integer productsQuantity,

        @NotNull(message = "CreatedAt is mandatory")
        LocalDateTime createdAt,

        @NotNull(message = "ClosedAt is mandatory")
        LocalDateTime closedAt
) {
}

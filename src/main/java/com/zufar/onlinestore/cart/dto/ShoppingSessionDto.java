package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public record ShoppingSessionDto(

        @NotNull(message = "Id is the mandatory attribute")
        UUID id,

        @NotNull(message = "UserId is the mandatory attribute")
        UUID userId,

        @NotNull(message = "Items is the mandatory attribute")
        Collection<ShoppingSessionItemDto> items,

        @NotNull(message = "ItemsQuantity is the mandatory attribute")
        Integer itemsQuantity,

        @NotNull(message = "ProductsQuantity is the mandatory attribute")
        Integer productsQuantity,

        @NotNull(message = "CreatedAt is the mandatory attribute")
        LocalDateTime createdAt,

        @NotNull(message = "ClosedAt is the mandatory attribute")
        LocalDateTime closedAt
) {
}

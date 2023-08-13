package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AddNewItemsToShoppingSessionRequest(

        @NotNull(message = "Items is the mandatory attribute")
        Set<NewShoppingSessionItemDto> items
) {
}

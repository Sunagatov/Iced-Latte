package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddNewItemsToShoppingSessionRequest(

        @NotNull(message = "Items is the mandatory attribute")
        List<NewShoppingSessionItemDto> items
) {
}

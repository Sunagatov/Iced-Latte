package com.zufar.onlinestore.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetShoppingSessionRequest(

        @NotNull(message = "UserId is the mandatory attribute")
        UUID userId
) {
}

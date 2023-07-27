package com.zufar.onlinestore.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductResponseDto(

        UUID id,

        String name,

        String description,

        PriceDetailsDto priceDetails,

        Integer quantity
) {
}
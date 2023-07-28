package com.zufar.onlinestore.product.dto;

import java.util.UUID;

public record ProductResponseDto(

        UUID id,

        String name,

        String description,

        PriceDetailsDto priceDetails,

        Integer quantity
) {
}
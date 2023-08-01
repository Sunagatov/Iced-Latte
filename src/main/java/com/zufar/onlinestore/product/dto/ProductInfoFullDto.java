package com.zufar.onlinestore.product.dto;

import java.util.UUID;

public record ProductInfoFullDto(
        UUID id,
        String name,
        String description,
        PriceDetailsDto priceDetails,
        Integer quantity,
        Boolean active
) {
}

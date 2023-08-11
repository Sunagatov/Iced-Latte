package com.zufar.onlinestore.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductInfoDto(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity
) {
}

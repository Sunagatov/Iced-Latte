package com.zufar.icedlatte.product.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSnapshot(
        UUID id,
        String name,
        BigDecimal price,
        String productFileUrl
) {
}

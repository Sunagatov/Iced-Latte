package com.zufar.icedlatte.product.api.dto;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSnapshot(
        UUID id,
        String name,
        @Nullable String description,
        BigDecimal price,
        @Nullable Integer quantity,
        @Nullable Boolean active,
        @Nullable String productFileUrl
) {
}

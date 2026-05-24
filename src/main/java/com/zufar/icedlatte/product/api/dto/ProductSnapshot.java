package com.zufar.icedlatte.product.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record ProductSnapshot(
        UUID id,
        String name,
        @Nullable String description,
        BigDecimal price,
        @Nullable Integer quantity,
        @Nullable Boolean active,
        @Nullable String productFileUrl) {}

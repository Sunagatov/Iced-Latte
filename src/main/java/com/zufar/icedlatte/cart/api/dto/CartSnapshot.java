package com.zufar.icedlatte.cart.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record CartSnapshot(
        UUID id,
        UUID userId,
        List<CartItemSnapshot> items,
        int itemsQuantity,
        BigDecimal itemsTotalPrice,
        int productsQuantity,
        OffsetDateTime createdAt,
        @Nullable OffsetDateTime closedAt) {}

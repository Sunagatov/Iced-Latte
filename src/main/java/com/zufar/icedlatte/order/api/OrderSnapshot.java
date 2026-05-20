package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderSnapshot(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal itemsTotalPrice,
        String stripePaymentIntentId,
        List<OrderItemSnapshot> items
) {
    public record OrderItemSnapshot(
            String productName,
            BigDecimal productPrice,
            int productsQuantity
    ) {}
}

package com.zufar.icedlatte.order.api;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderSnapshot(
        UUID id,
        UUID userId,
        OrderStatusSnapshot status,
        BigDecimal itemsTotalPrice,
        @Nullable String stripePaymentIntentId,
        List<OrderItemSnapshot> items
) {

    public record OrderItemSnapshot(
            String productName,
            BigDecimal productPrice,
            int productsQuantity
    ) {}

}

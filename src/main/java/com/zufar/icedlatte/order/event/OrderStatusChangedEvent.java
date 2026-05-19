package com.zufar.icedlatte.order.event;

import com.zufar.icedlatte.openapi.dto.OrderStatus;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        UUID changedBy,
        String reason,
        OffsetDateTime timestamp
) {
    public OrderStatusChangedEvent {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(newStatus, "newStatus");
        Objects.requireNonNull(timestamp, "timestamp");
    }
}

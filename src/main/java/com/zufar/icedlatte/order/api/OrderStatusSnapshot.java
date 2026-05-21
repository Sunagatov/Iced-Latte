package com.zufar.icedlatte.order.api;

public enum OrderStatusSnapshot {
    CREATED,
    PENDING_PAYMENT,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUND_REQUESTED,
    REFUNDED,
    PAYMENT_FAILED,
    PAYMENT_EXPIRED
}

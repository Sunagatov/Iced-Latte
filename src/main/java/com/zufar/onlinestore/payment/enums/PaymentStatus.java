package com.zufar.onlinestore.payment.enums;

public enum PaymentStatus {
    REQUIRES_PAYMENT_METHOD,
    REQUIRES_CONFIRMATION,
    REQUIRES_CAPTURE,
    REQUIRES_ACTION,
    PROCESSING,
    CANCELED,
    SUCCEEDED
}


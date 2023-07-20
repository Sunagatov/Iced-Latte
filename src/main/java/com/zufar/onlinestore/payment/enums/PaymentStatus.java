package com.zufar.onlinestore.payment.enums;

public enum PaymentStatus {
    REQUIRES_PAYMENT_METHOD, REQUIRES_CONFIRMATION, REQUIRES_ACTION,
    PROCESSING, REQUIRES_CAPTURE, CANCELED, SUCCEEDED
}

package com.zufar.onlinestore.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    REQUIRES_PAYMENT_METHOD("payment_intent.requires_payment_method",
            "Payment method is required"),

    REQUIRES_CONFIRMATION("payment_intent.requires_confirmation",
            "Payment confirmation is required"),

    REQUIRES_CAPTURE("payment_intent.requires_capture",
            "Payment capture required"),

    REQUIRES_ACTION("payment_intent.requires_action",
            "Additional action required to complete payment"),

    PAYMENT_FAILED("payment_intent.payment_failed",
            "Payment processing error"),

    PROCESSING("payment_intent.processing",
            "Payment in progress"),

    CANCELED("payment_intent.canceled",
            "Payment canceled"),

    SUCCEEDED("payment_intent.succeeded",
            "Payment succeeded");

    private final String status;
    private final String message;

}


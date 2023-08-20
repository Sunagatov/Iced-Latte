package com.zufar.onlinestore.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    PAYMENT_METHOD_IS_REQUIRED("payment_intent.requires_payment_method",
            "Payment method is required"),

    PAYMENT_CONFIRMATION_IS_REQUIRED("payment_intent.requires_confirmation",
            "Payment confirmation is required"),

    PAYMENT_CAPTURE_IS_REQUIRED("payment_intent.requires_capture",
            "Payment capture is required"),

    PAYMENT_ACTION_IS_REQUIRED("payment_intent.requires_action",
            "Additional action is required to complete payment"),

    PAYMENT_IS_FAILED("payment_intent.payment_failed",
            "Payment processing error"),

    PAYMENT_IS_PROCESSING("payment_intent.processing",
            "Payment in processing"),

    PAYMENT_IS_CANCELED("payment_intent.canceled",
            "Payment is canceled"),

    PAYMENT_IS_SUCCEEDED("payment_intent.succeeded",
            "Payment is succeeded");

    private final String status;
    private final String description;

}


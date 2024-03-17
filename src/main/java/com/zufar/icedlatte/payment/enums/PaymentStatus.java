package com.zufar.icedlatte.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    PAYMENT_METHOD_IS_REQUIRED("payment_intent.requires_payment_method",
            "Payment method is required."),

    PAYMENT_CONFIRMATION_IS_REQUIRED("payment_intent.requires_confirmation",
            "Payment confirmation is required."),

    PAYMENT_CAPTURE_IS_REQUIRED("payment_intent.requires_capture",
            "Payment capture is required."),

    PAYMENT_ACTION_IS_REQUIRED("payment_intent.requires_action",
            "Payment action is required."),

    PAYMENT_IS_FAILED("payment_intent.payment_failed",
            "Payment has failed."),

    PAYMENT_IN_PROCESSING("payment_intent.processing",
            "Payment is in processing."),

    PAYMENT_IS_CANCELED("payment_intent.canceled",
            "Payment has been canceled."),

    PAYMENT_IS_SUCCEEDED("payment_intent.succeeded",
            "Payment has succeeded.");

    private final String status;
    private final String description;
}


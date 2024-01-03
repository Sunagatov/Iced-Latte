package com.zufar.icedlatte.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    PAYMENT_METHOD_IS_REQUIRED("PAYMENT_METHOD_IS_REQUIRED",
            "Payment method is required."),

    PAYMENT_CONFIRMATION_IS_REQUIRED("PAYMENT_CONFIRMATION_IS_REQUIRED",
            "Payment confirmation is required."),

    PAYMENT_CAPTURE_IS_REQUIRED("PAYMENT_CAPTURE_IS_REQUIRED",
            "Payment capture is required."),

    PAYMENT_ACTION_IS_REQUIRED("PAYMENT_ACTION_IS_REQUIRED",
            "Payment action is required."),

    PAYMENT_IS_FAILED("PAYMENT_IS_FAILED",
            "Payment has failed."),

    PAYMENT_IN_PROCESSING("IN_PROCESSING",
            "Payment is in processing."),

    PAYMENT_IS_CANCELED("PAYMENT_IS_CANCELED",
            "Payment has been canceled."),

    PAYMENT_IS_SUCCEEDED("PAYMENT_IS_SUCCEEDED",
            "Payment has succeeded.");

    private final String status;
    private final String description;
}


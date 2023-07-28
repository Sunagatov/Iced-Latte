package com.zufar.onlinestore.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorMessage {

    REQUEST_BODY_ERROR("Error", "Request Body is mandatory"),

    ERROR_MESSAGE_IS_EMPTY("Error", "ErrorMessage Is Empty"),

    PAYMENT_NOT_FOUND_ERROR("Error", "Failed To Find a Payment"),

    PAYMENT_INTENT_PROCESSING_ERROR("Error", "Failed to Process a Payment Intent"),

    PAYMENT_EVENT_PROCESSING_ERROR("Error", "Failed to Process a Payment Event"),

    PAYMENT_METHOD_PROCESSING_ERROR("Error", "Failed to Process a Payment Method");


    private final String cause;

    private final String message;

}
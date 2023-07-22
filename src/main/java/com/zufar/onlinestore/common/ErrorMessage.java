package com.zufar.onlinestore.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorMessage {

    REQUEST_BODY_ERROR("Error", "Request Body is mandatory"),

    ERROR_MESSAGE_IS_EMPTY("Error", "ErrorMessage Is Empty"),

    PAYMENT_NOT_FOUND_ERROR("Error", "Failed To Find a Payment"),

    PAYMENT_PROCESSING_ERROR("Error", "Failed to Process a Payment");

    private final String cause;

    private final String message;

}

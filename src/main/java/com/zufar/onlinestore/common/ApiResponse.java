package com.zufar.onlinestore.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class ApiResponse {

    private final String message;

    private final String description;

    private final Boolean success;

    private final String timestamp;

    public ApiResponse(String message, String description, Boolean success) {
        this.message = message;
        this.description = description;
        this.success = success;
        this.timestamp = Instant.now()
                .toString();
    }
}

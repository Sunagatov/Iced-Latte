package com.zufar.onlinestore.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class ApiResponse {

    private final String data;

    private final Boolean success;

    private final String timestamp;

    public ApiResponse(String data, Boolean success) {
        this.data = data;
        this.success = success;
        this.timestamp = Instant.now()
                .toString();
    }
}

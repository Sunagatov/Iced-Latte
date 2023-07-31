package com.zufar.onlinestore.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ApiResponse<T>(

        @JsonProperty("data")
        T data,

        @JsonProperty("message")
        String message,

        @JsonProperty("httpStatusCode")
        Integer httpStatusCode,

        @JsonProperty("timestamp")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TIMESTAMP_JSON_FORMAT)
        LocalDateTime timestamp
) {
    public static final String TIMESTAMP_JSON_FORMAT = "yyyy-MM-dd HH:mm:ss";
}

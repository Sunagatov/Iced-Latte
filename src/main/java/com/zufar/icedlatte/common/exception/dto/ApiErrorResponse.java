package com.zufar.icedlatte.common.exception.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record ApiErrorResponse(

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

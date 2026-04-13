package com.zufar.icedlatte.common.exception.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Singular;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(

        @JsonProperty("message")
        String message,

        @JsonProperty("httpStatusCode")
        Integer httpStatusCode,

        @JsonProperty("timestamp")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = TIMESTAMP_JSON_FORMAT)
        LocalDateTime timestamp,

        @Singular
        @JsonProperty("errors")
        List<FieldError> errors
) {
    public static final String TIMESTAMP_JSON_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /** Backward-compatible 3-arg constructor used by existing tests and handlers. */
    public ApiErrorResponse(String message, Integer httpStatusCode, LocalDateTime timestamp) {
        this(message, httpStatusCode, timestamp, List.of());
    }

    public record FieldError(
            @JsonProperty("field") String field,
            @JsonProperty("message") String message
    ) {}
}

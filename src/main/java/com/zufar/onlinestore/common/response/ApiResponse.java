package com.zufar.onlinestore.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ApiResponse<T>(

        @JsonProperty("data")
        T data,

        @JsonProperty("message")
        String message,

        @JsonProperty("status")
        Integer status,

        @JsonProperty("time_stamp")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timeStamp
) {
}

package com.zufar.onlinestore.common.exception.handler;

import com.zufar.onlinestore.common.exception.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ApiErrorResponseCreator {

    public ApiErrorResponse buildResponse(Exception exception,
                                          HttpStatus httpStatus) {
        return ApiErrorResponse.builder()
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now())
                .httpStatusCode(httpStatus.value())
                .build();
    }
}

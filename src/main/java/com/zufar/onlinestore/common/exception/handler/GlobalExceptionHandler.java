package com.zufar.onlinestore.common.exception.handler;

import com.zufar.onlinestore.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    protected static final String DESCRIPTION_TEMPLATE = "Error message: %s. Operation was failed in method: %s at line number: %d from the class: %s.";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handleMethodArgumentNotValidException(final MethodArgumentNotValidException exception) {
        ApiResponse<String> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle method argument not valid exception: failed: message: {}.", apiResponse.message());
        return apiResponse;
    }

    protected ApiResponse<String> buildResponse(Exception exception, HttpStatus httpStatus) {
        return ApiResponse.<String>builder()
                .message(getErrorMessage(exception))
                .timestamp(LocalDateTime.now())
                .httpStatusCode(httpStatus.value())
                .build();
    }

    private String getErrorMessage(Exception exception) {
        return Arrays.stream(exception.getStackTrace())
                .findFirst()
                .map(topElement -> {
                    String errorMessage = exception instanceof MethodArgumentNotValidException methodArgumentNotValidException ?
                            methodArgumentNotValidException.getBindingResult().getAllErrors().stream()
                            .map(DefaultMessageSourceResolvable::getDefaultMessage)
                            .collect(Collectors.joining(".")) : exception.getMessage();

                    return DESCRIPTION_TEMPLATE.formatted(errorMessage, topElement.getMethodName(),
                            topElement.getLineNumber(), topElement.getClassName());
                })
                .orElse(StringUtils.EMPTY);
    }
}

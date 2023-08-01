package com.zufar.onlinestore.payment.exception.handler;

import com.zufar.onlinestore.common.response.ApiResponse;
import com.zufar.onlinestore.payment.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.Arrays;

@RequiredArgsConstructor
@RestControllerAdvice
@Slf4j
public class PaymentExceptionsHandler {

    private final String DESCRIPTION_TEMPLATE = "Operation was failed in method: %s from the class: %s.";

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<String> handlePaymentNotFoundException(final PaymentNotFoundException exception) {
        ApiResponse<String> apiResponse = buildResponse(exception, HttpStatus.NOT_FOUND);
        log.error("Handle payment not found exception: failed: message: {}, description: {}.", apiResponse.message(), apiResponse.data());
        return apiResponse;

    }

    @ExceptionHandler(PaymentEventProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handlePaymentEventProcessingException(final PaymentEventProcessingException exception) {
        ApiResponse<String> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment event processing exception: failed: message: {}, description: {}.", apiResponse.message(), apiResponse.data());
        return apiResponse;
    }

    @ExceptionHandler(PaymentIntentProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handlePaymentIntentProcessingException(final PaymentIntentProcessingException exception) {
        ApiResponse<String> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment intent processing exception: failed: message: {}, description: {}.", apiResponse.message(), apiResponse.data());
        return apiResponse;
    }

    @ExceptionHandler(PaymentMethodProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handlePaymentMethodProcessingException(final PaymentMethodProcessingException exception) {
        ApiResponse<String> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment method processing exception: failed: message: {}, description: {}.", apiResponse.message(), apiResponse.data());
        return apiResponse;
    }


    @ExceptionHandler(PaymentEventParsingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handlePaymentEventParsingException(final PaymentEventParsingException exception) {
        ApiResponse<String> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment event parsing exception: failed: message: {}, description: {}.", apiResponse.message(), apiResponse.data());
        return apiResponse;
    }

    private ApiResponse<String> buildResponse(Exception exception, HttpStatus httpStatus) {
        return ApiResponse.<String>builder()
                .data(getExceptionDescription(exception))
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now())
                .httpStatusCode(httpStatus.value())
                .build();
    }

    private String getExceptionDescription(Exception exception) {
        return Arrays.stream(exception.getStackTrace())
                .findFirst()
                .map(topElement -> DESCRIPTION_TEMPLATE.formatted(topElement.getMethodName(), topElement.getClassName()))
                .orElseGet(String::new);
    }
}

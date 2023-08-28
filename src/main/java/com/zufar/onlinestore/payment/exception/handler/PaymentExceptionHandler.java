package com.zufar.onlinestore.payment.exception.handler;

import com.zufar.onlinestore.common.exception.handler.GlobalExceptionHandler;
import com.zufar.onlinestore.common.response.ApiResponse;
import com.zufar.onlinestore.payment.exception.PaymentEventProcessingException;
import com.zufar.onlinestore.payment.exception.PaymentIntentProcessingException;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import com.zufar.onlinestore.payment.exception.PaymentMethodProcessingException;
import com.zufar.onlinestore.payment.exception.PaymentEventParsingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RequiredArgsConstructor
@RestControllerAdvice
@Slf4j
public class PaymentExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<String> handlePaymentNotFoundException(final PaymentNotFoundException exception) {
        ApiResponse<String> apiResponse = buildResponse(exception, HttpStatus.NOT_FOUND);
        log.error("Handle payment not found exception: failed: message: {}.", apiResponse.message());
        return apiResponse;
    }

    @ExceptionHandler(PaymentEventProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handlePaymentEventProcessingException(final PaymentEventProcessingException exception) {
        ApiResponse<String> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment event processing exception: failed: message: {}.", apiResponse.message());
        return apiResponse;
    }

    @ExceptionHandler(PaymentIntentProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handlePaymentIntentProcessingException(final PaymentIntentProcessingException exception) {
        ApiResponse<String> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment intent processing exception: failed: message: {}.", apiResponse.message());
        return apiResponse;
    }

    @ExceptionHandler(PaymentMethodProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handlePaymentMethodProcessingException(final PaymentMethodProcessingException exception) {
        ApiResponse<String> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment method processing exception: failed: message: {}.", apiResponse.message());
        return apiResponse;
    }

    @ExceptionHandler(PaymentEventParsingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handlePaymentEventParsingException(final PaymentEventParsingException exception) {
        ApiResponse<String> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment event parsing exception: failed: message: {}.", apiResponse.message());
        return apiResponse;
    }
}

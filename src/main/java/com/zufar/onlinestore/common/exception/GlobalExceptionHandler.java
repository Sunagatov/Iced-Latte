package com.zufar.onlinestore.common.exception;

import com.zufar.onlinestore.common.ErrorApiResponse;
import com.zufar.onlinestore.payment.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zufar.onlinestore.common.ErrorMessage.*;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleMethodArgumentNotValidException(final MethodArgumentNotValidException exception) {

        return exception.getBindingResult()
                .getAllErrors()
                .stream()
                .collect(Collectors.toMap(error -> ((FieldError) error).getField(), this::resolveErrorMessage));
    }

    private String resolveErrorMessage(ObjectError error) {
        String errorMessage = error.getDefaultMessage();
        return (errorMessage == null || errorMessage.isBlank()) ? ERROR_MESSAGE_IS_EMPTY.getDescription() : errorMessage;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleHttpMessageNotReadableException() {
        var errors = new HashMap<String, String>();
        errors.put(REQUEST_BODY_ERROR.getCause(), REQUEST_BODY_ERROR.getDescription());
        return errors;
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorApiResponse handlePaymentNotFoundException(final PaymentNotFoundException exception) {
        String message = exception.getMessage();
        log.error("Handle payment not found exception: failed: exception = {}", message);
        return ErrorApiResponse.builder()
                .message(exception.getMessage())
                .description(PAYMENT_NOT_FOUND_ERROR.getDescription())
                .time(Instant.now())
                .build();
    }

    @ExceptionHandler(PaymentEventProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorApiResponse handlePaymentEventProcessingException(final PaymentEventProcessingException exception) {
        String message = exception.getMessage();
        log.error("Handle payment event processing exception: failed: exception = {}", message);
        return ErrorApiResponse.builder()
                .message(exception.getMessage())
                .description(PAYMENT_EVENT_PROCESSING_ERROR.getDescription())
                .time(Instant.now())
                .build();
    }

    @ExceptionHandler(PaymentIntentProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorApiResponse handlePaymentIntentProcessingException(final PaymentIntentProcessingException exception) {
        String message = exception.getMessage();
        log.error("Handle payment intent processing exception: failed: exception = {}", message);
        return ErrorApiResponse.builder()
                .message(exception.getMessage())
                .description(PAYMENT_INTENT_PROCESSING_ERROR.getDescription())
                .time(Instant.now())
                .build();
    }

    @ExceptionHandler(PaymentMethodProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorApiResponse handlePaymentMethodProcessingException(final PaymentMethodProcessingException exception) {
        String message = exception.getMessage();
        log.error("Handle payment method processing exception: failed: exception = {}", message);
        return ErrorApiResponse.builder()
                .message(exception.getMessage())
                .description(PAYMENT_METHOD_PROCESSING_ERROR.getDescription())
                .time(Instant.now())
                .build();
    }

    @ExceptionHandler(PaymentEventParsingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorApiResponse handlePaymentEventParsingException(final PaymentEventParsingException exception) {
        String message = exception.getMessage();
        log.error("Handle payment event parsing exception: failed: exception = {}", message);
        return ErrorApiResponse.builder()
                .message(exception.getMessage())
                .description(PAYMENT_EVENT_PARSING_ERROR.getDescription())
                .time(Instant.now())
                .build();
    }

}

package com.zufar.onlinestore.common.exception;

import com.zufar.onlinestore.common.ApiResponse;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import com.zufar.onlinestore.payment.exception.PaymentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
    public ApiResponse handlePaymentNotFoundException(final PaymentNotFoundException exception) {
        String message = exception.getMessage();
        log.error("handlePaymentNotFoundException: failed: exception = {}", message, exception);
        return new ApiResponse(message, PAYMENT_NOT_FOUND_ERROR.getDescription(), false);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handlePaymentProcessingException(final PaymentProcessingException exception) {
        String message = exception.getMessage();
        log.error("handlePaymentProcessingException: failed: exception = {}", message, exception);
        return new ApiResponse(message, PAYMENT_PROCESSING_ERROR.getDescription(), false);
    }
}

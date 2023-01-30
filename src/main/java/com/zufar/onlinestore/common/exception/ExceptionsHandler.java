package com.zufar.onlinestore.common.exception;

import com.amazonaws.util.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class ExceptionsHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(final MethodArgumentNotValidException exception) {
        Map<String, String> errors = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .collect(Collectors.toMap(error -> ((FieldError) error).getField(),
                        error -> {
                            String errorMessage = error.getDefaultMessage();
                            if (StringUtils.isNullOrEmpty(errorMessage)) {
                                errorMessage = "ErrorMessage is empty";
                            }
                            return errorMessage;
                        }));

        return ResponseEntity.badRequest()
                .body(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException() {
        Map<String, String> errors = new HashMap<>();
        errors.put("Error message", "Request body is mandatory");
        return ResponseEntity.badRequest()
                .body(errors);
    }
}

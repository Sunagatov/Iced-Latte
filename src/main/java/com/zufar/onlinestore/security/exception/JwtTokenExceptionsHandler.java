package com.zufar.onlinestore.security.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.http.HttpStatusCode;

@ControllerAdvice
public class JwtTokenExceptionsHandler {

    @ExceptionHandler(JwtTokenException.class)
    public ResponseEntity<Map<String, String>> handleJwtTokenException(final JwtTokenException exception) {
        Map<String, String> errors = new HashMap<>();
        errors.put("JwtToken Error message", exception.getMessage());
        errors.put("JwtToken Cause Error message", exception.getCause().getMessage());
        return ResponseEntity.status(HttpStatusCode.UNAUTHORIZED)
                .body(errors);
    }
}

package com.zufar.onlinestore.review.advice;

import com.zufar.onlinestore.review.controller.ApiResponse;
import com.zufar.onlinestore.review.exception.ReviewDeleteFailedException;
import com.zufar.onlinestore.review.exception.ReviewNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class ReviewControllerAdvice {

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleReviewNotFoundException(ReviewNotFoundException ex) {
        log.error("Error occurred: {}", ex.getMessage());
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .timeStamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ReviewDeleteFailedException.class)
    public ResponseEntity<ApiResponse<String>> handleReviewDeleteFailedException(ReviewDeleteFailedException ex) {
        log.error("Error occurred: {}", ex.getMessage());
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .message(ex.getMessage())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timeStamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
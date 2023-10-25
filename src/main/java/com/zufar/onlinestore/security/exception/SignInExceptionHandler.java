package com.zufar.onlinestore.security.exception;

import com.zufar.onlinestore.common.exception.handler.GlobalExceptionHandler;
import com.zufar.onlinestore.common.response.ApiResponse;
import com.zufar.onlinestore.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RequiredArgsConstructor
@RestControllerAdvice
@Slf4j
public class SignInExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleUserNotFoundException(final UserNotFoundException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.NOT_FOUND);
        log.error("Handle user not found exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());
        return apiResponse;
    }

    @ExceptionHandler(UserAccountLockedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleUserAccountLockedException(final UserAccountLockedException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.UNAUTHORIZED);
        log.error("Handle user account locked exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());
        return apiResponse;
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleBadCredentialsException(final BadCredentialsException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.UNAUTHORIZED);
        log.error("Handle bad credentials exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());
        return apiResponse;
    }
}


package com.zufar.icedlatte.security.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.JwtTokenException;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(0)
public class JwtTokenExceptionsHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(JwtTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleJwtTokenException(final JwtTokenException exception) {
        return apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JwtTokenBlacklistedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleJwtTokenBlacklistedException(final JwtTokenBlacklistedException exception) {
        log.warn("auth.refresh.rejected: reason={}, status=401", exception.getMessage());
        return apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JwtTokenHasNoUserEmailException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleJwtTokenHasNoUserEmailException(final JwtTokenHasNoUserEmailException exception) {
        log.warn("auth.refresh.rejected: reason=invalid_token, status=401");
        return apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED);
    }
}

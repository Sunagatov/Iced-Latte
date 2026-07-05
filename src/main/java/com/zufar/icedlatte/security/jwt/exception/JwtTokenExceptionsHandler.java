package com.zufar.icedlatte.security.jwt.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(0)
public class JwtTokenExceptionsHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(JwtTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleJwtTokenException(final JwtTokenException exception) {
        log.debug("auth.rejected: reason=invalid_token, status=401");
        return problemDetailFactory.build(
                ProblemType.AUTH_FAILED, "Authentication failed", HttpStatus.UNAUTHORIZED, "Authentication failed.");
    }

    @ExceptionHandler(JwtTokenBlacklistedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleJwtTokenBlacklistedException(final JwtTokenBlacklistedException exception) {
        log.debug("auth.refresh.rejected: reason=token_invalidated, status=401");
        return problemDetailFactory.build(
                ProblemType.SESSION_EXPIRED,
                "Session expired",
                HttpStatus.UNAUTHORIZED,
                "Session expired. Please sign in again.");
    }

    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleExpiredJwtException(final ExpiredJwtException exception) {
        log.debug("auth.rejected: reason=token_expired, status=401");
        return problemDetailFactory.build(
                ProblemType.SESSION_EXPIRED,
                "Session expired",
                HttpStatus.UNAUTHORIZED,
                "Session expired. Please sign in again.");
    }
}

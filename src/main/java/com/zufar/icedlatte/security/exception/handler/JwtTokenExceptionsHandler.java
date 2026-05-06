package com.zufar.icedlatte.security.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.JwtTokenException;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(0)
public class JwtTokenExceptionsHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(JwtTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleJwtTokenException(final JwtTokenException exception) {
        return problemDetailFactory.build("auth-failed", "Authentication failed",
                HttpStatus.UNAUTHORIZED, "Authentication failed.");
    }

    @ExceptionHandler(JwtTokenBlacklistedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleJwtTokenBlacklistedException(final JwtTokenBlacklistedException exception) {
        log.debug("auth.refresh.rejected: reason=token_invalidated, status=401");
        return problemDetailFactory.build(ProblemType.SESSION_EXPIRED, "Session expired",
                HttpStatus.UNAUTHORIZED, "Session expired. Please sign in again.");
    }

    @ExceptionHandler(JwtTokenHasNoUserEmailException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleJwtTokenHasNoUserEmailException(final JwtTokenHasNoUserEmailException exception) {
        log.debug("auth.refresh.rejected: reason=invalid_token, status=401");
        return problemDetailFactory.build("auth-failed", "Authentication failed",
                HttpStatus.UNAUTHORIZED, "Authentication failed.");
    }
}

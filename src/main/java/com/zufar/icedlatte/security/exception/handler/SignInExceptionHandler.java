package com.zufar.icedlatte.security.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.security.exception.InvalidCredentialsException;
import com.zufar.icedlatte.security.exception.UserAccountLockedException;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(0)
public class SignInExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(UserRegistrationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleUserRegistrationException(final UserRegistrationException exception) {
        return handle(exception, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleInvalidCredentialsException(final InvalidCredentialsException exception) {
        return handle(exception, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({UserNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleUserNotFoundException(final UserNotFoundException exception) {
        return handle(exception, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    // amazonq-ignore-next-line
    public ApiErrorResponse handleUsernameNotFoundException(final UsernameNotFoundException exception) {
        return handle(exception, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserAccountLockedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleUserAccountLockedException(final UserAccountLockedException exception) {
        return handle(exception, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleBadCredentialsException(final BadCredentialsException exception) {
        return handle(exception, HttpStatus.UNAUTHORIZED);
    }

    private ApiErrorResponse handle(Exception exception, HttpStatus status) {
        ApiErrorResponse response = apiErrorResponseCreator.buildResponse(exception, status);
        log.warn("exception.sign_in.{}: message={}",
                exception.getClass().getSimpleName(), response.message());
        return response;
    }
}


package com.zufar.icedlatte.security.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.security.configuration.AuthPaths;
import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.exception.InvalidCredentialsException;
import com.zufar.icedlatte.security.exception.UserAccountLockedException;
import com.zufar.icedlatte.security.exception.SessionNotFoundException;
import com.zufar.icedlatte.security.exception.SessionOwnershipException;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
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
@SuppressWarnings("unused")
public class SignInExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(AbsentBearerHeaderException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleAbsentBearerHeaderException(final AbsentBearerHeaderException exception,
                                                              HttpServletRequest request) {
        ApiErrorResponse response = apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED);
        String method = request.getMethod();
        String path = sanitize(request.getRequestURI());

        if (!AuthPaths.REFRESH.equals(path)) {
            log.debug("auth.sign_in.failed: reason_code={}, status=401, method={}, path={}",
                    exception.getClass().getSimpleName(), method, path);
        }

        return response;
    }

    @ExceptionHandler(UserRegistrationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleUserRegistrationException(final UserRegistrationException exception,
                                                            HttpServletRequest request) {
        return handle(exception, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleInvalidCredentialsException(final InvalidCredentialsException exception,
                                                              HttpServletRequest request) {
        return handle(exception, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({UserNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleUserNotFoundException(final UserNotFoundException exception,
                                                        HttpServletRequest request) {
        return handle(exception, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    // amazonq-ignore-next-line
    public ApiErrorResponse handleUsernameNotFoundException(final UsernameNotFoundException exception,
                                                            HttpServletRequest request) {
        return handle(exception, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(UserAccountLockedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleUserAccountLockedException(final UserAccountLockedException exception,
                                                             HttpServletRequest request) {
        return handle(exception, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleBadCredentialsException(final BadCredentialsException exception,
                                                          HttpServletRequest request) {
        return handle(exception, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleSessionNotFoundException(final SessionNotFoundException exception,
                                                           HttpServletRequest request) {
        log.debug("auth.session.not_found: method={}, path={}", request.getMethod(), sanitize(request.getRequestURI()));
        return apiErrorResponseCreator.buildResponse("Session not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SessionOwnershipException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleSessionOwnershipException(final SessionOwnershipException exception,
                                                            HttpServletRequest request) {
        log.debug("auth.session.forbidden: method={}, path={}", request.getMethod(), sanitize(request.getRequestURI()));
        return apiErrorResponseCreator.buildResponse("Access denied", HttpStatus.FORBIDDEN);
    }

    private ApiErrorResponse handle(Exception exception,
                                    HttpStatus status,
                                    HttpServletRequest request) {
        ApiErrorResponse response = apiErrorResponseCreator.buildResponse(exception, status);
        log.debug("auth.sign_in.failed: reason_code={}, status={}, method={}, path={}",
                exception.getClass().getSimpleName(),
                status.value(),
                request.getMethod(),
                sanitize(request.getRequestURI()));
        return response;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]", "_");
    }
}

package com.zufar.icedlatte.security.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
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
import org.springframework.http.ProblemDetail;
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

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(AbsentBearerHeaderException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleAbsentBearerHeaderException(final AbsentBearerHeaderException exception,
                                                           HttpServletRequest request) {
        if (!AuthPaths.REFRESH.equals(request.getRequestURI())) {
            log.debug("auth.sign_in.failed: reason_code=AbsentBearerHeaderException, status=401, method={}, path={}",
                    request.getMethod(), sanitize(request.getRequestURI()));
        }
        return problemDetailFactory.build("auth-required", "Authentication required",
                HttpStatus.UNAUTHORIZED, "Authentication required.");
    }

    @ExceptionHandler(UserRegistrationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleUserRegistrationException(final UserRegistrationException exception,
                                                         HttpServletRequest request) {
        logAuthFailure(exception, HttpStatus.CONFLICT, request);
        return problemDetailFactory.build("registration-failed", "Registration failed",
                HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleInvalidCredentialsException(final InvalidCredentialsException exception,
                                                           HttpServletRequest request) {
        logAuthFailure(exception, HttpStatus.UNAUTHORIZED, request);
        return problemDetailFactory.build("invalid-credentials", "Invalid credentials",
                HttpStatus.UNAUTHORIZED, "The login credentials are invalid.");
    }

    @ExceptionHandler({UserNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleUserNotFoundException(final UserNotFoundException exception,
                                                     HttpServletRequest request) {
        logAuthFailure(exception, HttpStatus.UNAUTHORIZED, request);
        return problemDetailFactory.build("invalid-credentials", "Invalid credentials",
                HttpStatus.UNAUTHORIZED, "The login credentials are invalid.");
    }

    @ExceptionHandler({UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleUsernameNotFoundException(final UsernameNotFoundException exception,
                                                         HttpServletRequest request) {
        logAuthFailure(exception, HttpStatus.UNAUTHORIZED, request);
        return problemDetailFactory.build("invalid-credentials", "Invalid credentials",
                HttpStatus.UNAUTHORIZED, "The login credentials are invalid.");
    }

    @ExceptionHandler(UserAccountLockedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleUserAccountLockedException(final UserAccountLockedException exception,
                                                          HttpServletRequest request) {
        logAuthFailure(exception, HttpStatus.UNAUTHORIZED, request);
        return problemDetailFactory.build("account-locked", "Account locked",
                HttpStatus.UNAUTHORIZED, "User account is locked.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleBadCredentialsException(final BadCredentialsException exception,
                                                       HttpServletRequest request) {
        logAuthFailure(exception, HttpStatus.UNAUTHORIZED, request);
        return problemDetailFactory.build("invalid-credentials", "Invalid credentials",
                HttpStatus.UNAUTHORIZED, "The login credentials are invalid.");
    }

    @ExceptionHandler(SessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleSessionNotFoundException(final SessionNotFoundException exception,
                                                        HttpServletRequest request) {
        log.debug("auth.session.not_found: method={}, path={}", request.getMethod(), sanitize(request.getRequestURI()));
        return problemDetailFactory.build("session-not-found", "Session not found",
                HttpStatus.NOT_FOUND, "Session not found.");
    }

    @ExceptionHandler(SessionOwnershipException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleSessionOwnershipException(final SessionOwnershipException exception,
                                                         HttpServletRequest request) {
        log.debug("auth.session.forbidden: method={}, path={}", request.getMethod(), sanitize(request.getRequestURI()));
        return problemDetailFactory.build("session-access-denied", "Access denied",
                HttpStatus.FORBIDDEN, "Access denied.");
    }

    private void logAuthFailure(Exception exception, HttpStatus status, HttpServletRequest request) {
        log.debug("auth.sign_in.failed: reason_code={}, status={}, method={}, path={}",
                exception.getClass().getSimpleName(), status.value(),
                request.getMethod(), sanitize(request.getRequestURI()));
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]", "_");
    }
}

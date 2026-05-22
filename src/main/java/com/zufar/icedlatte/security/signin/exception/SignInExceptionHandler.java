package com.zufar.icedlatte.security.signin.exception;

import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.common.http.RequestPathUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(AuthSecurityException.class)
    public ResponseEntity<ProblemDetail> handleSecurityException(final AuthSecurityException ex,
                                                                 HttpServletRequest request) {
        record ErrorMapping(String logTag, String typeSlug, String title, HttpStatus status, String detail) { }

        var mapping = switch (ex) {
            case AbsentBearerHeaderException _ ->
                    new ErrorMapping("auth.sign_in.failed.AbsentBearerHeaderException", ProblemType.AUTH_REQUIRED,
                            "Authentication required", HttpStatus.UNAUTHORIZED, "Authentication required.");
            case UserRegistrationException _ ->
                    new ErrorMapping("auth.sign_in.failed.UserRegistrationException", ProblemType.REGISTRATION_FAILED,
                            "Registration failed", HttpStatus.CONFLICT, ex.getMessage());
            case InvalidCredentialsException _ ->
                    new ErrorMapping("auth.sign_in.failed.InvalidCredentialsException", ProblemType.INVALID_CREDENTIALS,
                            "Invalid credentials", HttpStatus.UNAUTHORIZED, "The login credentials are invalid.");
            case UserAccountLockedException _ ->
                    new ErrorMapping("auth.sign_in.failed.UserAccountLockedException", ProblemType.ACCOUNT_LOCKED,
                            "Account locked", HttpStatus.UNAUTHORIZED, "User account is locked.");
            case SessionNotFoundException _ ->
                    new ErrorMapping("auth.session.not_found", ProblemType.SESSION_NOT_FOUND,
                            "Session not found", HttpStatus.NOT_FOUND, "Session not found.");
            case SessionOwnershipException _ ->
                    new ErrorMapping("auth.session.forbidden", ProblemType.SESSION_ACCESS_DENIED,
                            "Access denied", HttpStatus.FORBIDDEN, "Access denied.");
            case TurnstileVerificationException _ ->
                    new ErrorMapping("auth.turnstile.failed", ProblemType.TURNSTILE_FAILED,
                            "Verification failed", HttpStatus.BAD_REQUEST, ex.getMessage());
        };

        if (ex instanceof AbsentBearerHeaderException &&
                ApiPaths.AUTH_REFRESH.equals(request.getRequestURI())) {
            // Suppress noisy logging for missing bearer on refresh endpoint
        } else {
            log.debug("{}: status={}, method={}, path={}", mapping.logTag(), mapping.status().value(),
                    request.getMethod(), RequestPathUtils.sanitize(request.getRequestURI()));
        }

        ProblemDetail pd = problemDetailFactory.build(mapping.typeSlug(), mapping.title(), mapping.status(), mapping.detail());
        return ResponseEntity.status(mapping.status()).body(pd);
    }

    @ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleSpringSecurityCredentialExceptions(final Exception exception,
                                                                  HttpServletRequest request) {
        log.debug("auth.sign_in.failed: reason_code={}, status=401, method={}, path={}",
                exception.getClass().getSimpleName(), request.getMethod(), RequestPathUtils.sanitize(request.getRequestURI()));
        return problemDetailFactory.build(ProblemType.INVALID_CREDENTIALS, "Invalid credentials",
                HttpStatus.UNAUTHORIZED, "The login credentials are invalid.");
    }

}

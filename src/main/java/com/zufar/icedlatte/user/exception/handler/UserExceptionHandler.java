package com.zufar.icedlatte.user.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class UserExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(InvalidAvatarFileTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleInvalidAvatarFileTypeException(final InvalidAvatarFileTypeException exception) {
        log.debug("exception.avatar.invalid_type: exceptionClass={}, status=400", exception.getClass().getSimpleName());
        return problemDetailFactory.build("invalid-avatar-type", "Invalid file type",
                HttpStatus.BAD_REQUEST, "Invalid file type. Allowed types: JPEG, PNG, WebP");
    }

    @ExceptionHandler({UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleUsernameNotFoundException(final UsernameNotFoundException exception) {
        log.debug("exception.user.username_not_found: exceptionClass={}, status=401", exception.getClass().getSimpleName());
        return problemDetailFactory.build("auth-failed", "Authentication failed",
                HttpStatus.UNAUTHORIZED, "User not found.");
    }
}

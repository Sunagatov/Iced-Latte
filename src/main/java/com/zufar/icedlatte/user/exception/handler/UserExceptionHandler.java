package com.zufar.icedlatte.user.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import com.zufar.icedlatte.user.exception.UserException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class UserExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ProblemDetail> handleUserException(final UserException ex) {
        record ErrorMapping(String logTag, String typeSlug, String title, HttpStatus status, String detail) {}

        var mapping = switch (ex) {
            case UserNotFoundException _ ->
                    new ErrorMapping("exception.user.not_found", "user-not-found", "User not found", HttpStatus.NOT_FOUND, ex.getMessage());
            case InvalidAvatarFileTypeException _ ->
                    new ErrorMapping("exception.avatar.invalid_type", "invalid-avatar-type", "Invalid file type", HttpStatus.BAD_REQUEST, "Invalid file type. Allowed types: JPEG, PNG, WebP");
        };

        HttpStatus httpStatus = mapping.status();
        log.debug("{}: status={}", mapping.logTag(), httpStatus.value());
        return ResponseEntity.status(httpStatus)
                .body(problemDetailFactory.build(mapping.typeSlug(), mapping.title(), httpStatus, mapping.detail()));
    }
}

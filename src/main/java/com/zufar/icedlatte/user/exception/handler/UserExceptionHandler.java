package com.zufar.icedlatte.user.exception.handler;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import com.zufar.icedlatte.user.exception.UserAvatarUploadException;
import com.zufar.icedlatte.user.exception.UserException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class UserExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ProblemDetail> handleUserException(final UserException ex) {
        return switch (ex) {
            case UserNotFoundException _ ->
                problem(
                        "exception.user.not_found",
                        ProblemType.USER_NOT_FOUND,
                        "User not found",
                        HttpStatus.NOT_FOUND,
                        ex.getMessage());
            case InvalidAvatarFileTypeException _ ->
                problem(
                        "exception.avatar.invalid_type",
                        ProblemType.INVALID_AVATAR_TYPE,
                        "Invalid file type",
                        HttpStatus.BAD_REQUEST,
                        "Invalid file type. Allowed types: JPEG, PNG, WebP");
            case UserAvatarUploadException _ ->
                problem(
                        "exception.avatar.upload_failed",
                        ProblemType.FILE_UPLOAD_FAILED,
                        "File upload failed",
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Avatar upload is currently unavailable.");
        };
    }

    private ResponseEntity<ProblemDetail> problem(
            String logTag, String typeSlug, String title, HttpStatus status, String detail) {
        log.debug("{}: status={}", logTag, status.value());
        return ResponseEntity.status(status).body(problemDetailFactory.build(typeSlug, title, status, detail));
    }
}

package com.zufar.icedlatte.user.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class UserExceptionHandler {

    private static final String INVALID_AVATAR_FILE_TYPE_MESSAGE = "Invalid file type. Allowed types: JPEG, PNG, WebP";
    private static final String USERNAME_NOT_FOUND_MESSAGE = "User not found";

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(InvalidAvatarFileTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidAvatarFileTypeException(final InvalidAvatarFileTypeException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                INVALID_AVATAR_FILE_TYPE_MESSAGE, HttpStatus.BAD_REQUEST);
        log.debug("exception.avatar.invalid_type: exceptionClass={}, status=400", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler({UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleUsernameNotFoundException(final UsernameNotFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(USERNAME_NOT_FOUND_MESSAGE, HttpStatus.UNAUTHORIZED);
        log.debug("exception.user.username_not_found: exceptionClass={}, status=401", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

}

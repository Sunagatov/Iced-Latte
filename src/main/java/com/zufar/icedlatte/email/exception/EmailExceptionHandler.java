package com.zufar.icedlatte.email.exception;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(0)
public class EmailExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidTokenException(final InvalidTokenException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.warn("exception.email.invalid_token: message={}", apiErrorResponse.message());
        return apiErrorResponse;
    }

    @ExceptionHandler(MessageBuilderNotFoundException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleMessageBuilderNotFoundException(final MessageBuilderNotFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR);
        log.error("exception.email.builder_not_found: message={}", exception.getMessage(), exception);
        return apiErrorResponse;
    }

    @ExceptionHandler(TimeTokenException.class)
    @ResponseStatus(HttpStatus.TOO_EARLY)
    public ApiErrorResponse handleTimeTokenException(final TimeTokenException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.warn("exception.email.time_token: message={}", apiErrorResponse.message());
        return apiErrorResponse;
    }

    @ExceptionHandler(IncorrectTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleTokenTimeExpiredException(final IncorrectTokenException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.warn("exception.email.token_expired: message={}", apiErrorResponse.message());
        return apiErrorResponse;
    }

    @ExceptionHandler(IncorrectTokenFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleIncorrectTokenFormatException(final IncorrectTokenFormatException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.warn("exception.email.invalid_token_format: message={}", apiErrorResponse.message());
        return apiErrorResponse;
    }
}

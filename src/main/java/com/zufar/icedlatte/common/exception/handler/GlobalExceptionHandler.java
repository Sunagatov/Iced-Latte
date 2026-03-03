package com.zufar.icedlatte.common.exception.handler;

import com.zufar.icedlatte.common.exception.ResourceNotFoundException;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMethodArgumentNotValidException(final MethodArgumentNotValidException exception) {
        String message = exception
                .getBindingResult()
                .getAllErrors().stream()
                .map(error -> String.format("{ ErrorMessage: %s }", error))
                .toList()
                .toString();

        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(message, HttpStatus.BAD_REQUEST);
        log.warn("exception.method_argument_invalid: message={}", message);

        return apiErrorResponse;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleResourceNotFoundException(final ResourceNotFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);

        log.warn("exception.resource_not_found: message={}", apiErrorResponse.message());

        return apiErrorResponse;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleConstraintViolationException(final ConstraintViolationException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.warn("exception.constraint_violation: message={}", apiErrorResponse.message());
        return apiErrorResponse;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNoResourceFoundException(final NoResourceFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);
        log.warn("exception.resource_not_found: method={}, path={}", exception.getHttpMethod(), exception.getResourcePath());
        return apiErrorResponse;
    }

    @ExceptionHandler(JwtTokenHasNoUserEmailException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleJwtTokenHasNoUserEmailException(final JwtTokenHasNoUserEmailException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED);
        log.warn("exception.auth.invalid_token: message={}", exception.getMessage());
        return apiErrorResponse;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnhandledException(final Exception exception) throws Exception {
        if (exception instanceof MethodArgumentTypeMismatchException || exception instanceof HttpMessageNotReadableException) {
            throw exception;
        }
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR);
        log.error("exception.unhandled: message={}", exception.getMessage(), exception);
        return apiErrorResponse;
    }
}

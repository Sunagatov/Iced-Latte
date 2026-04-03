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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMethodArgumentNotValidException(final MethodArgumentNotValidException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                exception.getMessage(), HttpStatus.BAD_REQUEST);
        log.warn("exception.method_argument_invalid: errorCount={}, status=400",
                exception.getBindingResult().getErrorCount());
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
        log.warn("exception.constraint_violation: errorCount={}, status=400",
                exception.getConstraintViolations().size());
        return apiErrorResponse;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNoResourceFoundException(final NoResourceFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);
        log.info("exception.resource_not_found: method={}, path={}", exception.getHttpMethod(), exception.getResourcePath());
        return apiErrorResponse;
    }

    @ExceptionHandler(JwtTokenHasNoUserEmailException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleJwtTokenHasNoUserEmailException(final JwtTokenHasNoUserEmailException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED);
        log.warn("exception.auth.invalid_token: message={}", exception.getMessage());
        return apiErrorResponse;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    public ApiErrorResponse handleMaxUploadSizeExceededException(final MaxUploadSizeExceededException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                "Uploaded file is too large",
                HttpStatus.CONTENT_TOO_LARGE
        );
        log.warn("exception.multipart.max_size_exceeded: exceptionClass={}, status=413",
                exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMultipartException(final MultipartException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                "Malformed multipart request",
                HttpStatus.BAD_REQUEST
        );
        log.warn("exception.multipart.invalid_request: exceptionClass={}, status=400",
                exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnhandledException(final Exception exception) throws Exception {
        if (exception instanceof MethodArgumentTypeMismatchException || exception instanceof HttpMessageNotReadableException) {
            throw exception;
        }
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR);
        log.error("exception.unhandled: exceptionClass={}, status=500", exception.getClass().getName(), exception);
        return apiErrorResponse;
    }
}

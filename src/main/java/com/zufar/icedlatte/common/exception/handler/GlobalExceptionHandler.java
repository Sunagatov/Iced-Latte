package com.zufar.icedlatte.common.exception.handler;

import com.zufar.icedlatte.common.exception.ResourceNotFoundException;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMethodArgumentNotValidException(final MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        String fieldNames = fieldErrors.stream()
                .map(ApiErrorResponse.FieldError::field)
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
        log.warn("exception.validation: errorCount={}, fields={}, status=400",
                exception.getBindingResult().getErrorCount(), fieldNames);
        return ApiErrorResponse.builder()
                .message("Validation failed")
                .httpStatusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(java.time.LocalDateTime.now())
                .errors(fieldErrors)
                .build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleResourceNotFoundException(final ResourceNotFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);
        log.warn("exception.resource_not_found: exceptionClass={}, status=404",
                exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleConstraintViolationException(final ConstraintViolationException exception) {
        List<ApiErrorResponse.FieldError> fieldErrors = exception.getConstraintViolations().stream()
                .map(v -> new ApiErrorResponse.FieldError(
                        v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        String fieldNames = fieldErrors.stream()
                .map(ApiErrorResponse.FieldError::field)
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
        log.warn("exception.constraint_violation: errorCount={}, fields={}, status=400",
                exception.getConstraintViolations().size(), fieldNames);
        return ApiErrorResponse.builder()
                .message("Validation failed")
                .httpStatusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(java.time.LocalDateTime.now())
                .errors(fieldErrors)
                .build();
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNoResourceFoundException(final NoResourceFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);
        log.warn("exception.resource_not_found: method={}, path={}", exception.getHttpMethod(), exception.getResourcePath());
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                "Invalid value for parameter '" + exception.getName() + "'", HttpStatus.BAD_REQUEST);
        log.warn("exception.type_mismatch: param={}, value={}, status=400",
                exception.getName(), exception.getValue());
        return apiErrorResponse;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleHttpMessageNotReadableException(final HttpMessageNotReadableException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                "Malformed or unreadable request body", HttpStatus.BAD_REQUEST);
        log.warn("exception.message_not_readable: status=400");
        return apiErrorResponse;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMissingServletRequestParameterException(final MissingServletRequestParameterException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                "Required parameter '" + exception.getParameterName() + "' is missing", HttpStatus.BAD_REQUEST);
        log.warn("exception.missing_param: param={}, status=400", exception.getParameterName());
        return apiErrorResponse;
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public void handleAsyncRequestNotUsableException(final AsyncRequestNotUsableException exception) {
        // Client disconnected before response was flushed (broken pipe). Not a server error — suppress Sentry noise.
        log.debug("exception.client_disconnect: cause={}", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnhandledException(final Exception exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR);
        log.error("exception.unhandled: exceptionClass={}, status=500", exception.getClass().getName(), exception);
        return apiErrorResponse;
    }
}

package com.zufar.icedlatte.common.exception.handler;

import com.zufar.icedlatte.common.exception.ResourceNotFoundException;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
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
    @SuppressWarnings("unused")
    public ApiErrorResponse handleMethodArgumentNotValidException(final MethodArgumentNotValidException exception) {
        List<ApiErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        String fieldNames = fieldErrors.stream()
                .map(ApiErrorResponse.FieldError::field)
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
        log.debug("exception.validation: errorCount={}, fields={}, status=400",
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
        log.debug("exception.resource_not_found: exceptionClass={}, status=404",
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
        log.debug("exception.constraint_violation: errorCount={}, fields={}, status=400",
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
        String path = normalizePath(sanitize(exception.getResourcePath()));
        String method = exception.getHttpMethod().name();

        if (isPublicInternetNoise(path)) {
            log.debug("exception.resource_not_found.scan: method={}, path={}", method, path);
        } else {
            log.debug("exception.resource_not_found: method={}, path={}", method, path);
        }

        return apiErrorResponse;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    public ApiErrorResponse handleMaxUploadSizeExceededException(final MaxUploadSizeExceededException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                "Uploaded file is too large",
                HttpStatus.CONTENT_TOO_LARGE
        );
        log.debug("exception.multipart.max_size_exceeded: exceptionClass={}, status=413",
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
        log.debug("exception.multipart.invalid_request: exceptionClass={}, status=400",
                exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                "Invalid value for parameter '" + exception.getName() + "'", HttpStatus.BAD_REQUEST);
        log.debug("exception.type_mismatch: param={}, status=400",
                exception.getName());
        return apiErrorResponse;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleHttpMessageNotReadableException(final HttpMessageNotReadableException ignored) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                "Malformed or unreadable request body", HttpStatus.BAD_REQUEST);
        log.debug("exception.message_not_readable: status=400");
        return apiErrorResponse;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMissingServletRequestParameterException(final MissingServletRequestParameterException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                "Required parameter '" + exception.getParameterName() + "' is missing", HttpStatus.BAD_REQUEST);
        log.debug("exception.missing_param: param={}, status=400", exception.getParameterName());
        return apiErrorResponse;
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public void handleAsyncRequestNotUsableException(final AsyncRequestNotUsableException exception) {
        // Client disconnected before response was flushed (broken pipe). Not a server error — suppress Sentry noise.
        log.debug("exception.client_disconnect: cause={}", exception.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleHttpMediaTypeNotAcceptableException(
            final HttpMediaTypeNotAcceptableException exception) {
        log.debug("exception.not_acceptable: status=406, message={}", sanitize(exception.getMessage()));
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Void> handleHttpRequestMethodNotSupportedException(
            final HttpRequestMethodNotSupportedException exception) {
        log.debug("exception.method_not_supported: method={}, status=405",
                sanitize(exception.getMethod()));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleUnhandledException(final Exception exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR);
        log.error("exception.unhandled: exceptionClass={}, status=500", exception.getClass().getName(), exception);
        return apiErrorResponse;
    }

    private static boolean isPublicInternetNoise(String path) {
        return !path.startsWith("/api/")
                && !path.startsWith("/actuator/")
                && !path.startsWith("/api/docs/");
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "/";
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]", "_");
    }
}

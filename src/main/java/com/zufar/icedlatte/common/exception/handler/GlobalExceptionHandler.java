package com.zufar.icedlatte.common.exception.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

    private static final String DATA_INTEGRITY_MESSAGE = "Request conflicts with existing data.";

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @SuppressWarnings("unused")
    public ProblemDetail handleMethodArgumentNotValidException(final MethodArgumentNotValidException exception) {
        List<ProblemDetailFactory.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ProblemDetailFactory.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        String fieldNames = fieldErrors.stream()
                .map(ProblemDetailFactory.FieldError::field)
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
        log.debug("exception.validation: errorCount={}, fields={}, status=400",
                exception.getBindingResult().getErrorCount(), fieldNames);
        return problemDetailFactory.build("validation-failed", "Validation failed",
                HttpStatus.BAD_REQUEST, "Validation failed.", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleConstraintViolationException(final ConstraintViolationException exception) {
        List<ProblemDetailFactory.FieldError> fieldErrors = exception.getConstraintViolations().stream()
                .map(v -> new ProblemDetailFactory.FieldError(
                        v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        String fieldNames = fieldErrors.stream()
                .map(ProblemDetailFactory.FieldError::field)
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
        log.debug("exception.constraint_violation: errorCount={}, fields={}, status=400",
                exception.getConstraintViolations().size(), fieldNames);
        return problemDetailFactory.build("validation-failed", "Validation failed",
                HttpStatus.BAD_REQUEST, "Validation failed.", fieldErrors);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNoResourceFoundException(final NoResourceFoundException exception) {
        String path = normalizePath(sanitize(exception.getResourcePath()));
        String method = exception.getHttpMethod().name();
        if (isPublicInternetNoise(path)) {
            log.debug("exception.resource_not_found.scan: method={}, path={}", method, path);
        } else {
            log.debug("exception.resource_not_found: method={}, path={}", method, path);
        }
        return problemDetailFactory.build("resource-not-found", "Resource not found",
                HttpStatus.NOT_FOUND, "No resource found for " + method + " " + path);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    public ProblemDetail handleMaxUploadSizeExceededException(final MaxUploadSizeExceededException exception) {
        log.debug("exception.multipart.max_size_exceeded: exceptionClass={}, status=413",
                exception.getClass().getSimpleName());
        return problemDetailFactory.build("file-too-large", "File too large",
                HttpStatus.CONTENT_TOO_LARGE, "Uploaded file is too large.");
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMultipartException(final MultipartException exception) {
        log.debug("exception.multipart.invalid_request: exceptionClass={}, status=400",
                exception.getClass().getSimpleName());
        return problemDetailFactory.build("malformed-multipart", "Malformed request",
                HttpStatus.BAD_REQUEST, "Malformed multipart request.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException exception) {
        log.debug("exception.type_mismatch: param={}, status=400", exception.getName());
        return problemDetailFactory.build("invalid-parameter", "Invalid parameter",
                HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + exception.getName() + "'.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleHttpMessageNotReadableException(final HttpMessageNotReadableException ignored) {
        log.debug("exception.message_not_readable: status=400");
        return problemDetailFactory.build("malformed-request", "Malformed request",
                HttpStatus.BAD_REQUEST, "Malformed or unreadable request body.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingServletRequestParameterException(final MissingServletRequestParameterException exception) {
        log.debug("exception.missing_param: param={}, status=400", exception.getParameterName());
        return problemDetailFactory.build("missing-parameter", "Missing parameter",
                HttpStatus.BAD_REQUEST, "Required parameter '" + exception.getParameterName() + "' is missing.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleDataIntegrityViolationException(final DataIntegrityViolationException exception) {
        log.debug("exception.data_integrity: exceptionClass={}, status=400",
                exception.getClass().getSimpleName());
        return problemDetailFactory.build("data-conflict", "Data conflict",
                HttpStatus.BAD_REQUEST, DATA_INTEGRITY_MESSAGE);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public void handleAsyncRequestNotUsableException(final AsyncRequestNotUsableException exception) {
        log.debug("exception.client_disconnect: cause={}", exception.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotAcceptableException(
            final HttpMediaTypeNotAcceptableException exception) {
        log.debug("exception.not_acceptable: status=406, message={}", sanitize(exception.getMessage()));
        ProblemDetail pd = problemDetailFactory.build("about:blank", "Not Acceptable",
                HttpStatus.NOT_ACCEPTABLE, "The requested media type is not supported.");
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(pd);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupportedException(
            final HttpRequestMethodNotSupportedException exception) {
        log.debug("exception.method_not_supported: method={}, status=405", sanitize(exception.getMethod()));
        ProblemDetail pd = problemDetailFactory.build("about:blank", "Method Not Allowed",
                HttpStatus.METHOD_NOT_ALLOWED, "HTTP method '" + sanitize(exception.getMethod()) + "' is not supported.");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnhandledException(final Exception exception) {
        HttpStatus status = resolveHttpStatus(exception);
        String typeSlug = status.is5xxServerError() ? "internal-error" : "about:blank";
        String title = status.is5xxServerError() ? "Internal server error" : status.getReasonPhrase();

        ProblemDetail pd = problemDetailFactory.build(typeSlug, title, status, exception.getMessage());

        if (status.is5xxServerError()) {
            log.error("exception.unhandled: exceptionClass={}, status={}", exception.getClass().getName(), status.value(), exception);
        } else {
            log.debug("exception.annotated: exceptionClass={}, status={}",
                    exception.getClass().getSimpleName(), status.value());
        }

        return ResponseEntity.status(status).body(pd);
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

    private static HttpStatus resolveHttpStatus(Exception exception) {
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(
                exception.getClass(), ResponseStatus.class);
        return responseStatus != null ? responseStatus.code() : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

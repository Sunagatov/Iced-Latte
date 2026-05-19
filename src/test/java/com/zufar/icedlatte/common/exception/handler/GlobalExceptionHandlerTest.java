package com.zufar.icedlatte.common.exception.handler;

import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler unit tests")
class GlobalExceptionHandlerTest {

    @Mock
    private ProblemDetailFactory problemDetailFactory;

    @InjectMocks
    private GlobalExceptionHandler handler;

    private static ProblemDetail stub(int status) {
        return ProblemDetail.forStatus(status);
    }

    @Nested
    @DisplayName("generic exceptions")
    class GenericExceptions {

        @Test
        @DisplayName("returns 404 for missing static resource")
        void returns404ForMissingStaticResource() {
            NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/missing", "classpath:/static/");
            ProblemDetail expected = stub(404);
            when(problemDetailFactory.build(eq("resource-not-found"), eq("Resource not found"),
                    eq(HttpStatus.NOT_FOUND), any(String.class))).thenReturn(expected);

            ProblemDetail result = handler.handleNoResourceFoundException(ex);

            assertThat(result).isEqualTo(expected);
            verify(problemDetailFactory).build(eq("resource-not-found"), eq("Resource not found"),
                    eq(HttpStatus.NOT_FOUND), any(String.class));
        }

        @Test
        @DisplayName("returns 500 for unhandled exception")
        void returns500ForUnhandledException() {
            Exception ex = new RuntimeException("boom");
            ProblemDetail expected = stub(500);
            when(problemDetailFactory.build("internal-error", "Internal server error",
                    HttpStatus.INTERNAL_SERVER_ERROR, "boom")).thenReturn(expected);

            ResponseEntity<ProblemDetail> result = handler.handleUnhandledException(ex);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result.getBody()).isEqualTo(expected);
        }

        @Test
        @DisplayName("returns annotated status for domain exceptions")
        void returnsAnnotatedStatusForDomainExceptions() {
            ProductNotFoundException ex = new ProductNotFoundException(UUID.randomUUID());
            ProblemDetail expected = stub(404);
            when(problemDetailFactory.build(eq("about:blank"), eq("Not Found"),
                    eq(HttpStatus.NOT_FOUND), any(String.class))).thenReturn(expected);

            ResponseEntity<ProblemDetail> result = handler.handleUnhandledException(ex);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(result.getBody()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("validation exceptions")
    class ValidationExceptions {

        @Test
        @DisplayName("returns validation response for constraint violations")
        void returnsValidationResponseForConstraintViolations() {
            ConstraintViolation<?> violation = pageSizeViolation();
            ConstraintViolationException ex = new ConstraintViolationException("violation", Set.of(violation));
            ProblemDetail expected = stub(400);
            when(problemDetailFactory.build(eq("validation-failed"), eq("Validation failed"),
                    eq(HttpStatus.BAD_REQUEST), eq("Validation failed."), any())).thenReturn(expected);

            ProblemDetail result = handler.handleConstraintViolationException(ex);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("returns 400 for method argument type mismatch")
        void returns400ForMethodArgumentTypeMismatch() throws Exception {
            Method method = Object.class.getDeclaredMethod("toString");
            MethodParameter parameter = new MethodParameter(method, -1);
            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                    "p1", String.class, "id", parameter, new RuntimeException());
            ProblemDetail expected = stub(400);
            when(problemDetailFactory.build("invalid-parameter", "Invalid parameter",
                    HttpStatus.BAD_REQUEST, "Invalid value for parameter 'id'.")).thenReturn(expected);

            ProblemDetail result = handler.handleMethodArgumentTypeMismatchException(ex);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("returns 400 for unreadable request body")
        void returns400ForUnreadableRequestBody() {
            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException("bad body", new MockHttpInputMessage(new byte[0]));
            ProblemDetail expected = stub(400);
            when(problemDetailFactory.build("malformed-request", "Malformed request",
                    HttpStatus.BAD_REQUEST, "Malformed or unreadable request body.")).thenReturn(expected);

            ProblemDetail result = handler.handleHttpMessageNotReadableException(ex);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("returns 400 for missing request parameter")
        void returns400ForMissingRequestParameter() {
            MissingServletRequestParameterException ex =
                    new MissingServletRequestParameterException("page", "Integer");
            ProblemDetail expected = stub(400);
            when(problemDetailFactory.build("missing-parameter", "Missing parameter",
                    HttpStatus.BAD_REQUEST, "Required parameter 'page' is missing.")).thenReturn(expected);

            ProblemDetail result = handler.handleMissingServletRequestParameterException(ex);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("returns 400 for data integrity violations")
        void returns400ForDataIntegrityViolations() {
            DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicate key");
            ProblemDetail expected = stub(400);
            when(problemDetailFactory.build("data-conflict", "Data conflict",
                    HttpStatus.BAD_REQUEST, "Request conflicts with existing data.")).thenReturn(expected);

            ProblemDetail result = handler.handleDataIntegrityViolationException(ex);

            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("multipart and transport exceptions")
    class MultipartAndTransportExceptions {

        @Test
        @DisplayName("returns 413 for oversized upload")
        void returns413ForOversizedUpload() {
            MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1024);
            ProblemDetail expected = stub(413);
            when(problemDetailFactory.build("file-too-large", "File too large",
                    HttpStatus.CONTENT_TOO_LARGE, "Uploaded file is too large.")).thenReturn(expected);

            ProblemDetail result = handler.handleMaxUploadSizeExceededException(ex);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("returns 400 for malformed multipart request")
        void returns400ForMalformedMultipartRequest() {
            MultipartException ex = new MultipartException("broken multipart");
            ProblemDetail expected = stub(400);
            when(problemDetailFactory.build("malformed-multipart", "Malformed request",
                    HttpStatus.BAD_REQUEST, "Malformed multipart request.")).thenReturn(expected);

            ProblemDetail result = handler.handleMultipartException(ex);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("returns 406 for not acceptable")
        void returns406ForNotAcceptable() {
            ProblemDetail expected = stub(406);
            when(problemDetailFactory.build("about:blank", "Not Acceptable",
                    HttpStatus.NOT_ACCEPTABLE, "The requested media type is not supported.")).thenReturn(expected);

            ResponseEntity<ProblemDetail> result =
                    handler.handleHttpMediaTypeNotAcceptableException(new HttpMediaTypeNotAcceptableException("nope"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
            assertThat(result.getBody()).isEqualTo(expected);
        }

        @Test
        @DisplayName("returns 405 for unsupported method")
        void returns405ForUnsupportedMethod() {
            ProblemDetail expected = stub(405);
            when(problemDetailFactory.build(eq("about:blank"), eq("Method Not Allowed"),
                    eq(HttpStatus.METHOD_NOT_ALLOWED), any(String.class))).thenReturn(expected);

            ResponseEntity<ProblemDetail> result =
                    handler.handleHttpRequestMethodNotSupportedException(new HttpRequestMethodNotSupportedException("GET"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(result.getBody()).isEqualTo(expected);
        }

        @Test
        @DisplayName("suppresses async client disconnect exception")
        void suppressesAsyncClientDisconnectException() {
            handler.handleAsyncRequestNotUsableException(new AsyncRequestNotUsableException("broken pipe"));
        }
    }

    @SuppressWarnings("unchecked")
    private static ConstraintViolation<?> pageSizeViolation() {
        ConstraintViolation<Object> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
        Path propertyPath = org.mockito.Mockito.mock(Path.class);
        when(propertyPath.toString()).thenReturn("pageSize");
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn("must be greater than 0");
        return violation;
    }
}

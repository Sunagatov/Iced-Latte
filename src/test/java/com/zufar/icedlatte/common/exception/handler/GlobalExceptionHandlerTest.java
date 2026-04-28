package com.zufar.icedlatte.common.exception.handler;

import com.zufar.icedlatte.common.exception.ResourceNotFoundException;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler unit tests")
class GlobalExceptionHandlerTest {

    @Mock
    private ApiErrorResponseCreator apiErrorResponseCreator;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Nested
    @DisplayName("resource and generic exceptions")
    class ResourceAndGenericExceptions {

        @Test
        @DisplayName("returns 404 for resource not found")
        void returns404ForResourceNotFound() {
            ResourceNotFoundException ex = new ResourceNotFoundException("not found");
            when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.NOT_FOUND)).thenReturn(stub(404));

            ApiErrorResponse result = handler.handleResourceNotFoundException(ex);

            assertThat(result.httpStatusCode()).isEqualTo(404);
            verify(apiErrorResponseCreator).buildResponse(ex, HttpStatus.NOT_FOUND);
            verifyNoMoreInteractions(apiErrorResponseCreator);
        }

        @Test
        @DisplayName("returns 404 for missing static resource")
        void returns404ForMissingStaticResource() {
            NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/missing", null);
            when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.NOT_FOUND)).thenReturn(stub(404));

            ApiErrorResponse result = handler.handleNoResourceFoundException(ex);

            assertThat(result.httpStatusCode()).isEqualTo(404);
            verify(apiErrorResponseCreator).buildResponse(ex, HttpStatus.NOT_FOUND);
            verifyNoMoreInteractions(apiErrorResponseCreator);
        }

        @Test
        @DisplayName("returns 500 for unhandled exception")
        void returns500ForUnhandledException() {
            Exception ex = new RuntimeException("boom");
            when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR)).thenReturn(stub(500));

            ApiErrorResponse result = handler.handleUnhandledException(ex);

            assertThat(result.httpStatusCode()).isEqualTo(500);
            verify(apiErrorResponseCreator).buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
            verifyNoMoreInteractions(apiErrorResponseCreator);
        }
    }

    @Nested
    @DisplayName("validation exceptions")
    class ValidationExceptions {

        @Test
        @DisplayName("returns validation response for constraint violations")
        void returnsValidationResponseForConstraintViolations() {
            ConstraintViolation<?> violation = violation("pageSize", "must be greater than 0");
            ConstraintViolationException ex = new ConstraintViolationException("violation", Set.of(violation));

            ApiErrorResponse result = handler.handleConstraintViolationException(ex);

            assertThat(result.httpStatusCode()).isEqualTo(400);
            assertThat(result.message()).isEqualTo("Validation failed");
            assertThat(result.errors())
                    .singleElement()
                    .satisfies(error -> {
                        assertThat(error.field()).isEqualTo("pageSize");
                        assertThat(error.message()).isEqualTo("must be greater than 0");
                    });
        }

        @Test
        @DisplayName("returns 400 for method argument type mismatch")
        void returns400ForMethodArgumentTypeMismatch() throws Exception {
            Method method = Object.class.getDeclaredMethod("toString");
            MethodParameter parameter = new MethodParameter(method, -1);
            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                    "p1", String.class, "id", parameter, new RuntimeException());
            when(apiErrorResponseCreator.buildResponse("Invalid value for parameter 'id'", HttpStatus.BAD_REQUEST))
                    .thenReturn(stub(400));

            ApiErrorResponse result = handler.handleMethodArgumentTypeMismatchException(ex);

            assertThat(result.httpStatusCode()).isEqualTo(400);
            verify(apiErrorResponseCreator).buildResponse("Invalid value for parameter 'id'", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 400 for unreadable request body")
        void returns400ForUnreadableRequestBody() {
            HttpMessageNotReadableException ex =
                    new HttpMessageNotReadableException("bad body", new MockHttpInputMessage(new byte[0]));
            when(apiErrorResponseCreator.buildResponse("Malformed or unreadable request body", HttpStatus.BAD_REQUEST))
                    .thenReturn(stub(400));

            ApiErrorResponse result = handler.handleHttpMessageNotReadableException(ex);

            assertThat(result.httpStatusCode()).isEqualTo(400);
            verify(apiErrorResponseCreator).buildResponse("Malformed or unreadable request body", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 400 for missing request parameter")
        void returns400ForMissingRequestParameter() {
            MissingServletRequestParameterException ex =
                    new MissingServletRequestParameterException("page", "Integer");
            when(apiErrorResponseCreator.buildResponse("Required parameter 'page' is missing", HttpStatus.BAD_REQUEST))
                    .thenReturn(stub(400));

            ApiErrorResponse result = handler.handleMissingServletRequestParameterException(ex);

            assertThat(result.httpStatusCode()).isEqualTo(400);
            verify(apiErrorResponseCreator).buildResponse("Required parameter 'page' is missing", HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("multipart and transport exceptions")
    class MultipartAndTransportExceptions {

        @Test
        @DisplayName("returns 413 for oversized upload")
        void returns413ForOversizedUpload() {
            MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1024);
            when(apiErrorResponseCreator.buildResponse("Uploaded file is too large", HttpStatus.CONTENT_TOO_LARGE))
                    .thenReturn(stub(413));

            ApiErrorResponse result = handler.handleMaxUploadSizeExceededException(ex);

            assertThat(result.httpStatusCode()).isEqualTo(413);
            verify(apiErrorResponseCreator).buildResponse("Uploaded file is too large", HttpStatus.CONTENT_TOO_LARGE);
        }

        @Test
        @DisplayName("returns 400 for malformed multipart request")
        void returns400ForMalformedMultipartRequest() {
            MultipartException ex = new MultipartException("broken multipart");
            when(apiErrorResponseCreator.buildResponse("Malformed multipart request", HttpStatus.BAD_REQUEST))
                    .thenReturn(stub(400));

            ApiErrorResponse result = handler.handleMultipartException(ex);

            assertThat(result.httpStatusCode()).isEqualTo(400);
            verify(apiErrorResponseCreator).buildResponse("Malformed multipart request", HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 406 without body for not acceptable")
        void returns406WithoutBodyForNotAcceptable() {
            ResponseEntity<Void> result =
                    handler.handleHttpMediaTypeNotAcceptableException(new HttpMediaTypeNotAcceptableException("nope"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
            assertThat(result.getBody()).isNull();
        }

        @Test
        @DisplayName("returns 405 without body for unsupported method")
        void returns405WithoutBodyForUnsupportedMethod() {
            ResponseEntity<Void> result =
                    handler.handleHttpRequestMethodNotSupportedException(new HttpRequestMethodNotSupportedException("GET"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
            assertThat(result.getBody()).isNull();
        }

        @Test
        @DisplayName("suppresses async client disconnect exception")
        void suppressesAsyncClientDisconnectException() {
            handler.handleAsyncRequestNotUsableException(new AsyncRequestNotUsableException("broken pipe"));
        }
    }

    private static ApiErrorResponse stub(int status) {
        return new ApiErrorResponse("msg", status, LocalDateTime.now());
    }

    @SuppressWarnings("unchecked")
    private static ConstraintViolation<?> violation(String path, String message) {
        ConstraintViolation<Object> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
        Path propertyPath = org.mockito.Mockito.mock(Path.class);
        when(propertyPath.toString()).thenReturn(path);
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }
}

package com.zufar.icedlatte.common.exception.handler;

import com.zufar.icedlatte.common.exception.ResourceNotFoundException;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler unit tests")
class GlobalExceptionHandlerTest {

    @Mock
    private ApiErrorResponseCreator apiErrorResponseCreator;
    @InjectMocks
    private GlobalExceptionHandler handler;

    private ApiErrorResponse stub(int status) {
        return new ApiErrorResponse("msg", status, LocalDateTime.now());
    }

    @Test
    @DisplayName("handleResourceNotFoundException returns 404")
    void handleResourceNotFound_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("not found");
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.NOT_FOUND)).thenReturn(stub(404));

        ApiErrorResponse result = handler.handleResourceNotFoundException(ex);

        assertThat(result.httpStatusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("handleConstraintViolationException returns 400")
    void handleConstraintViolation_returns400() {
        ConstraintViolationException ex = new ConstraintViolationException("violation", Set.of());

        ApiErrorResponse result = handler.handleConstraintViolationException(ex);

        assertThat(result.httpStatusCode()).isEqualTo(400);
        assertThat(result.message()).isEqualTo("Validation failed");
    }

    @Test
    @DisplayName("handleUnhandledException returns 500 for generic exception")
    void handleUnhandled_returns500() {
        Exception ex = new RuntimeException("boom");
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR)).thenReturn(stub(500));

        ApiErrorResponse result = handler.handleUnhandledException(ex);

        assertThat(result.httpStatusCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatchException returns 400")
    void handleTypeMismatch_returns400() throws Exception {
        Method method = Object.class.getDeclaredMethod("toString");
        MethodParameter mp = new MethodParameter(method, -1);
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "p1", String.class, "id", mp, new RuntimeException());
        when(apiErrorResponseCreator.buildResponse("Invalid value for parameter 'id'", HttpStatus.BAD_REQUEST))
                .thenReturn(stub(400));

        ApiErrorResponse result = handler.handleMethodArgumentTypeMismatchException(ex);

        assertThat(result.httpStatusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("handleHttpMessageNotReadableException returns 400")
    void handleMessageNotReadable_returns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad body", new MockHttpInputMessage(new byte[0]));
        when(apiErrorResponseCreator.buildResponse("Malformed or unreadable request body", HttpStatus.BAD_REQUEST))
                .thenReturn(stub(400));

        ApiErrorResponse result = handler.handleHttpMessageNotReadableException(ex);

        assertThat(result.httpStatusCode()).isEqualTo(400);
    }
}

package com.zufar.icedlatte.common.exception.handler;

import com.zufar.icedlatte.common.exception.ResourceNotFoundException;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.BAD_REQUEST)).thenReturn(stub(400));

        ApiErrorResponse result = handler.handleConstraintViolationException(ex);

        assertThat(result.httpStatusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("handleJwtTokenHasNoUserEmailException returns 401")
    void handleJwtNoEmail_returns401() {
        JwtTokenHasNoUserEmailException ex = new JwtTokenHasNoUserEmailException("bad", new RuntimeException("bad"));
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.UNAUTHORIZED)).thenReturn(stub(401));

        ApiErrorResponse result = handler.handleJwtTokenHasNoUserEmailException(ex);

        assertThat(result.httpStatusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("handleUnhandledException returns 500 for generic exception")
    void handleUnhandled_returns500() throws Exception {
        Exception ex = new RuntimeException("boom");
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR)).thenReturn(stub(500));

        ApiErrorResponse result = handler.handleUnhandledException(ex);

        assertThat(result.httpStatusCode()).isEqualTo(500);
    }

    @Test
    @DisplayName("handleUnhandledException rethrows MethodArgumentTypeMismatchException")
    void handleUnhandled_rethrowsTypeMismatch() throws Exception {
        Method method = Object.class.getDeclaredMethod("toString");
        MethodParameter mp = new MethodParameter(method, -1);
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "val", String.class, "param", mp, new RuntimeException());

        assertThatThrownBy(() -> handler.handleUnhandledException(ex))
                .isInstanceOf(MethodArgumentTypeMismatchException.class);
    }
}

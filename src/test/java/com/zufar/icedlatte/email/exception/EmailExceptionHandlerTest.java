package com.zufar.icedlatte.email.exception;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailExceptionHandler unit tests")
class EmailExceptionHandlerTest {

    @Mock private ApiErrorResponseCreator apiErrorResponseCreator;
    @InjectMocks private EmailExceptionHandler handler;

    private ApiErrorResponse response(String msg, int status) {
        return new ApiErrorResponse(msg, status, LocalDateTime.now());
    }

    @Test
    @DisplayName("handleInvalidTokenException returns BAD_REQUEST")
    void handleInvalidTokenException() {
        InvalidTokenException ex = new InvalidTokenException("user@example.com");
        ApiErrorResponse expected = response("bad token", 400);
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.BAD_REQUEST)).thenReturn(expected);
        assertThat(handler.handleInvalidTokenException(ex)).isEqualTo(expected);
        verify(apiErrorResponseCreator).buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("handleMessageBuilderNotFoundException returns INTERNAL_SERVER_ERROR")
    void handleMessageBuilderNotFoundException() {
        MessageBuilderNotFoundException ex = new MessageBuilderNotFoundException("SomeClass");
        ApiErrorResponse expected = response("not found", 500);
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR)).thenReturn(expected);
        assertThat(handler.handleMessageBuilderNotFoundException(ex)).isEqualTo(expected);
        verify(apiErrorResponseCreator).buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("handleTimeTokenException returns BAD_REQUEST")
    void handleTimeTokenException() {
        TimeTokenException ex = new TimeTokenException("user@example.com", OffsetDateTime.now().plusMinutes(5));
        ApiErrorResponse expected = response("too early", 400);
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.BAD_REQUEST)).thenReturn(expected);
        assertThat(handler.handleTimeTokenException(ex)).isEqualTo(expected);
        verify(apiErrorResponseCreator).buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("handleTokenTimeExpiredException returns BAD_REQUEST")
    void handleIncorrectTokenException() {
        IncorrectTokenException ex = new IncorrectTokenException();
        ApiErrorResponse expected = response("expired", 400);
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.BAD_REQUEST)).thenReturn(expected);
        assertThat(handler.handleTokenTimeExpiredException(ex)).isEqualTo(expected);
        verify(apiErrorResponseCreator).buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("handleIncorrectTokenFormatException returns BAD_REQUEST")
    void handleIncorrectTokenFormatException() {
        IncorrectTokenFormatException ex = new IncorrectTokenFormatException("UUID");
        ApiErrorResponse expected = response("bad format", 400);
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.BAD_REQUEST)).thenReturn(expected);
        assertThat(handler.handleIncorrectTokenFormatException(ex)).isEqualTo(expected);
        verify(apiErrorResponseCreator).buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Time-token handler is annotated with TOO_EARLY while building BAD_REQUEST response")
    void timeTokenHandlerResponseStatusContractIsExplicit() throws NoSuchMethodException {
        Method method = EmailExceptionHandler.class.getMethod("handleTimeTokenException", TimeTokenException.class);
        ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);

        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.value()).isEqualTo(HttpStatus.TOO_EARLY);
    }
}

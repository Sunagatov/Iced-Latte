package com.zufar.icedlatte.security.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.JwtTokenException;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenExceptionsHandler Tests")
class JwtTokenExceptionsHandlerTest {

    @Mock
    private ApiErrorResponseCreator apiErrorResponseCreator;

    @InjectMocks
    private JwtTokenExceptionsHandler jwtTokenExceptionsHandler;

    @Test
    @DisplayName("Should return UNAUTHORIZED ApiErrorResponse when JwtTokenException is thrown")
    void shouldReturnUnauthorizedWhenJwtTokenExceptionThrown() {
        JwtTokenException exception = new JwtTokenException("Jwt token error message");
        ApiErrorResponse expected = ApiErrorResponse.builder()
                .message("Jwt token error message")
                .httpStatusCode(HttpStatus.UNAUTHORIZED.value())
                .timestamp(java.time.LocalDateTime.now())
                .build();
        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED)).thenReturn(expected);

        ApiErrorResponse response = jwtTokenExceptionsHandler.handleJwtTokenException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.httpStatusCode());
        assertEquals("Jwt token error message", response.message());
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when JwtTokenBlacklistedException is thrown")
    void shouldReturnUnauthorizedWhenJwtTokenBlacklistedExceptionThrown() {
        JwtTokenBlacklistedException exception = new JwtTokenBlacklistedException("token revoked");
        ApiErrorResponse expected = ApiErrorResponse.builder()
                .message("token revoked")
                .httpStatusCode(HttpStatus.UNAUTHORIZED.value())
                .timestamp(LocalDateTime.now())
                .build();
        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED)).thenReturn(expected);

        ApiErrorResponse response = jwtTokenExceptionsHandler.handleJwtTokenBlacklistedException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.httpStatusCode());
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when JwtTokenHasNoUserEmailException is thrown")
    void shouldReturnUnauthorizedWhenJwtTokenHasNoUserEmailExceptionThrown() {
        JwtTokenHasNoUserEmailException exception = new JwtTokenHasNoUserEmailException("no email");
        ApiErrorResponse expected = ApiErrorResponse.builder()
                .message("no email")
                .httpStatusCode(HttpStatus.UNAUTHORIZED.value())
                .timestamp(LocalDateTime.now())
                .build();
        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED)).thenReturn(expected);

        ApiErrorResponse response = jwtTokenExceptionsHandler.handleJwtTokenHasNoUserEmailException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.httpStatusCode());
    }
}

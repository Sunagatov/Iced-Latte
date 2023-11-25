package com.zufar.icedlatte.user.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.common.exception.handler.ErrorDebugMessageCreator;
import com.zufar.icedlatte.user.exception.InvalidOldPasswordException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserExceptionHandler Tests")
class UserExceptionHandlerTest {

    @Mock
    private ApiErrorResponseCreator apiErrorResponseCreator;

    @Mock
    private ErrorDebugMessageCreator errorDebugMessageCreator;

    @InjectMocks
    private UserExceptionHandler userExceptionHandler;

    @Test
    @DisplayName("Should return ApiErrorResponse with UNAUTHORIZED status when UserNotFoundException is thrown")
    void shouldReturnApiErrorResponseWithUnauthorizedStatusWhenUserNotFoundExceptionThrown() {
        UUID userId = UUID.randomUUID();
        LocalDateTime currentDateTime = LocalDateTime.now();
        UserNotFoundException exception = new UserNotFoundException(userId);
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "User with id = " + userId + " is not found.",
                HttpStatus.UNAUTHORIZED.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED)).thenReturn(expectedResponse);
        when(errorDebugMessageCreator.buildErrorDebugMessage(exception)).thenReturn("Error Debug Message");

        ApiErrorResponse actualResponse = userExceptionHandler.handleUserNotFoundException(exception);

        assertEquals(expectedResponse.httpStatusCode(), actualResponse.httpStatusCode());
        assertEquals(expectedResponse.message(), actualResponse.message());
        assertEquals(expectedResponse.timestamp(), actualResponse.timestamp());

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.UNAUTHORIZED);
        verify(errorDebugMessageCreator).buildErrorDebugMessage(exception);
    }

    @Test
    @DisplayName("Should return ApiErrorResponse with UNAUTHORIZED status when UsernameNotFoundException is thrown")
    void shouldReturnApiErrorResponseWithUnauthorizedStatusWhenUsernameNotFoundExceptionThrown() {
        UsernameNotFoundException exception = new UsernameNotFoundException("Username not found");
        LocalDateTime currentDateTime = LocalDateTime.now();
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "Username not found",
                HttpStatus.UNAUTHORIZED.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED)).thenReturn(expectedResponse);
        when(errorDebugMessageCreator.buildErrorDebugMessage(exception)).thenReturn("Error Debug Message");

        ApiErrorResponse actualResponse = userExceptionHandler.handleUsernameNotFoundException(exception);

        assertEquals(expectedResponse.httpStatusCode(), actualResponse.httpStatusCode());
        assertEquals(expectedResponse.message(), actualResponse.message());
        assertEquals(expectedResponse.timestamp(), actualResponse.timestamp());

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.UNAUTHORIZED);
        verify(errorDebugMessageCreator).buildErrorDebugMessage(exception);
    }

    @Test
    @DisplayName("Should return ApiErrorResponse with UNAUTHORIZED status when InvalidOldPasswordException is thrown")
    void shouldReturnApiErrorResponseWithUnauthorizedStatusWhenInvalidOldPasswordExceptionThrown() {
        InvalidOldPasswordException exception = new InvalidOldPasswordException("Invalid old password");
        LocalDateTime currentDateTime = LocalDateTime.now();
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "Invalid old password",
                HttpStatus.UNAUTHORIZED.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED)).thenReturn(expectedResponse);
        when(errorDebugMessageCreator.buildErrorDebugMessage(exception)).thenReturn("Error Debug Message");

        ApiErrorResponse actualResponse = userExceptionHandler.handleInvalidOldPasswordException(exception);

        assertEquals(expectedResponse.httpStatusCode(), actualResponse.httpStatusCode());
        assertEquals(expectedResponse.message(), actualResponse.message());
        assertEquals(expectedResponse.timestamp(), actualResponse.timestamp());

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.UNAUTHORIZED);
        verify(errorDebugMessageCreator).buildErrorDebugMessage(exception);
    }
}

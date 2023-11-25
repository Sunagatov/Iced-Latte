package com.zufar.icedlatte.security.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.common.exception.handler.ErrorDebugMessageCreator;
import com.zufar.icedlatte.security.exception.UserAccountLockedException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignInExceptionHandler Tests")
class SignInExceptionHandlerTest {

    @Mock
    private ApiErrorResponseCreator apiErrorResponseCreator;

    @Mock
    private ErrorDebugMessageCreator errorDebugMessageCreator;

    @InjectMocks
    private SignInExceptionHandler signInExceptionHandler;

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

        ApiErrorResponse actualResponse = signInExceptionHandler.handleUserNotFoundException(exception);

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

        ApiErrorResponse actualResponse = signInExceptionHandler.handleUsernameNotFoundException(exception);

        assertEquals(expectedResponse.httpStatusCode(), actualResponse.httpStatusCode());
        assertEquals(expectedResponse.message(), actualResponse.message());
        assertEquals(expectedResponse.timestamp(), actualResponse.timestamp());

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.UNAUTHORIZED);
        verify(errorDebugMessageCreator).buildErrorDebugMessage(exception);
    }

    @Test
    @DisplayName("Should return ApiErrorResponse with UNAUTHORIZED status when UserAccountLockedException is thrown")
    void shouldReturnApiErrorResponseWithUnauthorizedStatusWhenUserAccountLockedExceptionThrown() {
        int userAccountLockoutDurationMinutes = 30;
        String userEmail = "userEmail";
        UserAccountLockedException exception = new UserAccountLockedException(userEmail, userAccountLockoutDurationMinutes);
        LocalDateTime currentDateTime = LocalDateTime.now();
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                String.format("The request was rejected due to an incorrect number of login attempts for the user with email='%s'. " +
                        "Try again in %s minutes or reset your password", userEmail, userAccountLockoutDurationMinutes),
                HttpStatus.UNAUTHORIZED.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED)).thenReturn(expectedResponse);
        when(errorDebugMessageCreator.buildErrorDebugMessage(exception)).thenReturn("Error Debug Message");

        ApiErrorResponse actualResponse = signInExceptionHandler.handleUserAccountLockedException(exception);

        assertEquals(expectedResponse.httpStatusCode(), actualResponse.httpStatusCode());
        assertEquals(expectedResponse.message(), actualResponse.message());
        assertEquals(expectedResponse.timestamp(), actualResponse.timestamp());

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.UNAUTHORIZED);
        verify(errorDebugMessageCreator).buildErrorDebugMessage(exception);
    }

    @Test
    @DisplayName("Should return ApiErrorResponse with UNAUTHORIZED status when BadCredentialsException is thrown")
    void shouldReturnApiErrorResponseWithUnauthorizedStatusWhenBadCredentialsExceptionThrown() {
        BadCredentialsException exception = new BadCredentialsException("Bad credentials.");
        LocalDateTime currentDateTime = LocalDateTime.now();
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "Bad credentials.",
                HttpStatus.UNAUTHORIZED.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED)).thenReturn(expectedResponse);
        when(errorDebugMessageCreator.buildErrorDebugMessage(exception)).thenReturn("Error Debug Message");

        ApiErrorResponse actualResponse = signInExceptionHandler.handleBadCredentialsException(exception);

        assertEquals(expectedResponse.httpStatusCode(), actualResponse.httpStatusCode());
        assertEquals(expectedResponse.message(), actualResponse.message());
        assertEquals(expectedResponse.timestamp(), actualResponse.timestamp());

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.UNAUTHORIZED);
        verify(errorDebugMessageCreator).buildErrorDebugMessage(exception);
    }
}

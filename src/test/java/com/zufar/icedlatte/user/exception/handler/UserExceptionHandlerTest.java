package com.zufar.icedlatte.user.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserExceptionHandler Tests")
class UserExceptionHandlerTest {

    @Mock
    private ProblemDetailFactory problemDetailFactory;

    @InjectMocks
    private UserExceptionHandler userExceptionHandler;

    @Test
    @DisplayName("Should return ProblemDetail with UNAUTHORIZED status when UsernameNotFoundException is thrown")
    void shouldReturnUnauthorizedWhenUsernameNotFoundExceptionIsThrown() {
        UsernameNotFoundException exception = new UsernameNotFoundException("Email cannot be empty");
        ProblemDetail expected = ProblemDetail.forStatus(401);
        when(problemDetailFactory.build("auth-failed", "Authentication failed",
                HttpStatus.UNAUTHORIZED, "User not found.")).thenReturn(expected);

        ProblemDetail result = userExceptionHandler.handleUsernameNotFoundException(exception);

        assertThat(result).isEqualTo(expected);
        verify(problemDetailFactory).build("auth-failed", "Authentication failed",
                HttpStatus.UNAUTHORIZED, "User not found.");
    }

    @Test
    @DisplayName("Should map invalid avatar file type to BAD_REQUEST")
    void shouldHandleInvalidAvatarFileTypeException() {
        InvalidAvatarFileTypeException exception = new InvalidAvatarFileTypeException(
                "image/gif", List.of("image/jpeg", "image/png", "image/webp"));
        ProblemDetail expected = ProblemDetail.forStatus(400);
        when(problemDetailFactory.build("invalid-avatar-type", "Invalid file type",
                HttpStatus.BAD_REQUEST, "Invalid file type. Allowed types: JPEG, PNG, WebP")).thenReturn(expected);

        ProblemDetail result = userExceptionHandler.handleInvalidAvatarFileTypeException(exception);

        assertThat(result).isEqualTo(expected);
        verify(problemDetailFactory).build("invalid-avatar-type", "Invalid file type",
                HttpStatus.BAD_REQUEST, "Invalid file type. Allowed types: JPEG, PNG, WebP");
    }
}

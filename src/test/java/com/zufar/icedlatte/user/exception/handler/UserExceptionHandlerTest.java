package com.zufar.icedlatte.user.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserExceptionHandler Tests")
class UserExceptionHandlerTest {

    @Mock
    private ProblemDetailFactory problemDetailFactory;

    @InjectMocks
    private UserExceptionHandler userExceptionHandler;

    @Test
    @DisplayName("Should return NOT_FOUND for UserNotFoundException")
    void shouldReturnNotFoundWhenUserNotFoundExceptionIsThrown() {
        UserNotFoundException exception = new UserNotFoundException(UUID.randomUUID());
        ProblemDetail expected = ProblemDetail.forStatus(404);
        when(problemDetailFactory.build("user-not-found", "User not found",
                HttpStatus.NOT_FOUND, exception.getMessage())).thenReturn(expected);

        ResponseEntity<ProblemDetail> result = userExceptionHandler.handleUserException(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should return BAD_REQUEST for InvalidAvatarFileTypeException")
    void shouldHandleInvalidAvatarFileTypeException() {
        InvalidAvatarFileTypeException exception = new InvalidAvatarFileTypeException(
                "image/gif", List.of("image/jpeg", "image/png", "image/webp"));
        ProblemDetail expected = ProblemDetail.forStatus(400);
        when(problemDetailFactory.build("invalid-avatar-type", "Invalid file type",
                HttpStatus.BAD_REQUEST, "Invalid file type. Allowed types: JPEG, PNG, WebP")).thenReturn(expected);

        ResponseEntity<ProblemDetail> result = userExceptionHandler.handleUserException(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isEqualTo(expected);
    }
}

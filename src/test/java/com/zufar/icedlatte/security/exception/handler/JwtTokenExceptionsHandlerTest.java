package com.zufar.icedlatte.security.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
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
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenExceptionsHandler Tests")
class JwtTokenExceptionsHandlerTest {

    @Mock
    private ProblemDetailFactory problemDetailFactory;

    @InjectMocks
    private JwtTokenExceptionsHandler jwtTokenExceptionsHandler;

    @Test
    @DisplayName("Should return UNAUTHORIZED when JwtTokenException is thrown")
    void shouldReturnUnauthorizedWhenJwtTokenExceptionThrown() {
        JwtTokenException exception = new JwtTokenException("Jwt token error message");
        ProblemDetail expected = ProblemDetail.forStatus(401);
        when(problemDetailFactory.build("auth-failed", "Authentication failed",
                HttpStatus.UNAUTHORIZED, "Authentication failed.")).thenReturn(expected);

        ProblemDetail result = jwtTokenExceptionsHandler.handleJwtTokenException(exception);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when JwtTokenBlacklistedException is thrown")
    void shouldReturnUnauthorizedWhenJwtTokenBlacklistedExceptionThrown() {
        JwtTokenBlacklistedException exception = new JwtTokenBlacklistedException("token revoked");
        ProblemDetail expected = ProblemDetail.forStatus(401);
        when(problemDetailFactory.build("session-expired", "Session expired",
                HttpStatus.UNAUTHORIZED, "Session expired. Please sign in again.")).thenReturn(expected);

        ProblemDetail result = jwtTokenExceptionsHandler.handleJwtTokenBlacklistedException(exception);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when JwtTokenHasNoUserEmailException is thrown")
    void shouldReturnUnauthorizedWhenJwtTokenHasNoUserEmailExceptionThrown() {
        JwtTokenHasNoUserEmailException exception = new JwtTokenHasNoUserEmailException("no email");
        ProblemDetail expected = ProblemDetail.forStatus(401);
        when(problemDetailFactory.build("auth-failed", "Authentication failed",
                HttpStatus.UNAUTHORIZED, "Authentication failed.")).thenReturn(expected);

        ProblemDetail result = jwtTokenExceptionsHandler.handleJwtTokenHasNoUserEmailException(exception);

        assertThat(result).isEqualTo(expected);
    }
}

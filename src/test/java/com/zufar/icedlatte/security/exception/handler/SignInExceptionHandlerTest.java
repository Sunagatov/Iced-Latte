package com.zufar.icedlatte.security.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.exception.UserAccountLockedException;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignInExceptionHandler Tests")
class SignInExceptionHandlerTest {

    @Mock
    private ProblemDetailFactory problemDetailFactory;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private SignInExceptionHandler signInExceptionHandler;

    private static final ProblemDetail STUB_401 = ProblemDetail.forStatus(401);

    @Test
    @DisplayName("Should return CONFLICT status when UserRegistrationException is thrown")
    void shouldReturnConflictWhenUserRegistrationExceptionThrown() {
        UserRegistrationException exception = new UserRegistrationException("This email is already registered.");
        ProblemDetail stub409 = ProblemDetail.forStatus(409);
        when(problemDetailFactory.build("registration-failed", "Registration failed",
                HttpStatus.CONFLICT, "This email is already registered.")).thenReturn(stub409);

        ProblemDetail result = signInExceptionHandler.handleUserRegistrationException(exception, request);

        assertThat(result).isEqualTo(stub409);
    }

    @Test
    @DisplayName("Should return ProblemDetail with UNAUTHORIZED status when UserNotFoundException is thrown")
    void shouldReturnUnauthorizedWhenUserNotFoundExceptionThrown() {
        UserNotFoundException exception = new UserNotFoundException(UUID.randomUUID());
        when(problemDetailFactory.build("invalid-credentials", "Invalid credentials",
                HttpStatus.UNAUTHORIZED, "The login credentials are invalid.")).thenReturn(STUB_401);

        ProblemDetail result = signInExceptionHandler.handleUserNotFoundException(exception, request);

        assertThat(result).isEqualTo(STUB_401);
    }

    @Test
    @DisplayName("Should return ProblemDetail with UNAUTHORIZED status when UsernameNotFoundException is thrown")
    void shouldReturnUnauthorizedWhenUsernameNotFoundExceptionThrown() {
        UsernameNotFoundException exception = new UsernameNotFoundException("Username not found");
        when(problemDetailFactory.build("invalid-credentials", "Invalid credentials",
                HttpStatus.UNAUTHORIZED, "The login credentials are invalid.")).thenReturn(STUB_401);

        ProblemDetail result = signInExceptionHandler.handleUsernameNotFoundException(exception, request);

        assertThat(result).isEqualTo(STUB_401);
    }

    @Test
    @DisplayName("Should return ProblemDetail with UNAUTHORIZED status when UserAccountLockedException is thrown")
    void shouldReturnUnauthorizedWhenUserAccountLockedExceptionThrown() {
        UserAccountLockedException exception = new UserAccountLockedException(30);
        when(problemDetailFactory.build("account-locked", "Account locked",
                HttpStatus.UNAUTHORIZED, "User account is locked.")).thenReturn(STUB_401);

        ProblemDetail result = signInExceptionHandler.handleUserAccountLockedException(exception, request);

        assertThat(result).isEqualTo(STUB_401);
    }

    @Test
    @DisplayName("Should return ProblemDetail with UNAUTHORIZED status when BadCredentialsException is thrown")
    void shouldReturnUnauthorizedWhenBadCredentialsExceptionThrown() {
        BadCredentialsException exception = new BadCredentialsException("Bad credentials.");
        when(problemDetailFactory.build("invalid-credentials", "Invalid credentials",
                HttpStatus.UNAUTHORIZED, "The login credentials are invalid.")).thenReturn(STUB_401);

        ProblemDetail result = signInExceptionHandler.handleBadCredentialsException(exception, request);

        assertThat(result).isEqualTo(STUB_401);
    }

    @Test
    @DisplayName("Should return 401 when AbsentBearerHeaderException is thrown")
    void shouldReturnUnauthorizedWhenAbsentBearerHeaderExceptionThrown() {
        AbsentBearerHeaderException exception = new AbsentBearerHeaderException();
        when(problemDetailFactory.build("auth-required", "Authentication required",
                HttpStatus.UNAUTHORIZED, "Authentication required.")).thenReturn(STUB_401);

        ProblemDetail result = signInExceptionHandler.handleAbsentBearerHeaderException(exception, request);

        assertThat(result).isEqualTo(STUB_401);
    }
}

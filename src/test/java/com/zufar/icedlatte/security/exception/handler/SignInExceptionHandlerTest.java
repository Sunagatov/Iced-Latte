package com.zufar.icedlatte.security.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.exception.InvalidCredentialsException;
import com.zufar.icedlatte.security.exception.UserAccountLockedException;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignInExceptionHandler Tests")
class SignInExceptionHandlerTest {

    @Mock private ProblemDetailFactory problemDetailFactory;
    @Mock private HttpServletRequest request;
    @InjectMocks private SignInExceptionHandler handler;

    private static final ProblemDetail STUB_401 = ProblemDetail.forStatus(401);

    @Test
    @DisplayName("Should return CONFLICT when UserRegistrationException is thrown")
    void shouldReturnConflictWhenUserRegistrationExceptionThrown() {
        var ex = new UserRegistrationException("This email is already registered.");
        var stub409 = ProblemDetail.forStatus(409);
        when(problemDetailFactory.build("registration-failed", "Registration failed",
                HttpStatus.CONFLICT, "This email is already registered.")).thenReturn(stub409);

        var result = handler.handleSecurityException(ex, request);

        assertThat(result.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when UserAccountLockedException is thrown")
    void shouldReturnUnauthorizedWhenUserAccountLockedExceptionThrown() {
        var ex = new UserAccountLockedException(30);
        when(problemDetailFactory.build("account-locked", "Account locked",
                HttpStatus.UNAUTHORIZED, "User account is locked.")).thenReturn(STUB_401);

        var result = handler.handleSecurityException(ex, request);

        assertThat(result.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when InvalidCredentialsException is thrown")
    void shouldReturnUnauthorizedWhenInvalidCredentialsExceptionThrown() {
        var ex = new InvalidCredentialsException();
        when(problemDetailFactory.build("invalid-credentials", "Invalid credentials",
                HttpStatus.UNAUTHORIZED, "The login credentials are invalid.")).thenReturn(STUB_401);

        var result = handler.handleSecurityException(ex, request);

        assertThat(result.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when AbsentBearerHeaderException is thrown")
    void shouldReturnUnauthorizedWhenAbsentBearerHeaderExceptionThrown() {
        var ex = new AbsentBearerHeaderException();
        when(problemDetailFactory.build("auth-required", "Authentication required",
                HttpStatus.UNAUTHORIZED, "Authentication required.")).thenReturn(STUB_401);

        var result = handler.handleSecurityException(ex, request);

        assertThat(result.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when UsernameNotFoundException is thrown")
    void shouldReturnUnauthorizedWhenUsernameNotFoundExceptionThrown() {
        var ex = new UsernameNotFoundException("Username not found");
        when(problemDetailFactory.build("invalid-credentials", "Invalid credentials",
                HttpStatus.UNAUTHORIZED, "The login credentials are invalid.")).thenReturn(STUB_401);

        var result = handler.handleSpringSecurityCredentialExceptions(ex, request);

        assertThat(result).isEqualTo(STUB_401);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when BadCredentialsException is thrown")
    void shouldReturnUnauthorizedWhenBadCredentialsExceptionThrown() {
        var ex = new BadCredentialsException("Bad credentials.");
        when(problemDetailFactory.build("invalid-credentials", "Invalid credentials",
                HttpStatus.UNAUTHORIZED, "The login credentials are invalid.")).thenReturn(STUB_401);

        var result = handler.handleSpringSecurityCredentialExceptions(ex, request);

        assertThat(result).isEqualTo(STUB_401);
    }
}

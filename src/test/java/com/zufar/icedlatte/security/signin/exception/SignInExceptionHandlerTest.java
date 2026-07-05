package com.zufar.icedlatte.security.signin.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignInExceptionHandler Tests")
class SignInExceptionHandlerTest {

    @Mock
    private ProblemDetailFactory problemDetailFactory;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private SignInExceptionHandler handler;

    private static final ProblemDetail STUB_401 = ProblemDetail.forStatus(401);

    @Test
    @DisplayName("Should return CONFLICT when UserRegistrationException is thrown")
    void shouldReturnConflictWhenUserRegistrationExceptionThrown() {
        var ex = new UserRegistrationException("This email is already registered.");
        var stub409 = ProblemDetail.forStatus(409);
        when(problemDetailFactory.build(
                        "registration-failed",
                        "Registration failed",
                        HttpStatus.CONFLICT,
                        "This email is already registered."))
                .thenReturn(stub409);

        var result = handler.handleSecurityException(ex, request);

        assertThat(result.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when UserAccountLockedException is thrown")
    void shouldReturnUnauthorizedWhenUserAccountLockedExceptionThrown() {
        var ex = new UserAccountLockedException(30);
        when(problemDetailFactory.build(
                        "account-locked", "Account locked", HttpStatus.UNAUTHORIZED, "User account is locked."))
                .thenReturn(STUB_401);

        var result = handler.handleSecurityException(ex, request);

        assertThat(result.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when InvalidCredentialsException is thrown")
    void shouldReturnUnauthorizedWhenInvalidCredentialsExceptionThrown() {
        var ex = new InvalidCredentialsException();
        when(problemDetailFactory.build(
                        "invalid-credentials",
                        "Invalid credentials",
                        HttpStatus.UNAUTHORIZED,
                        "The login credentials are invalid."))
                .thenReturn(STUB_401);

        var result = handler.handleSecurityException(ex, request);

        assertThat(result.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when AbsentBearerHeaderException is thrown")
    void shouldReturnUnauthorizedWhenAbsentBearerHeaderExceptionThrown() {
        var ex = new AbsentBearerHeaderException();
        when(problemDetailFactory.build(
                        "auth-required",
                        "Authentication required",
                        HttpStatus.UNAUTHORIZED,
                        "Authentication required."))
                .thenReturn(STUB_401);

        var result = handler.handleSecurityException(ex, request);

        assertThat(result.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when UsernameNotFoundException is thrown")
    void shouldReturnUnauthorizedWhenUsernameNotFoundExceptionThrown() {
        var ex = new UsernameNotFoundException("Username not found");
        when(problemDetailFactory.build(
                        "invalid-credentials",
                        "Invalid credentials",
                        HttpStatus.UNAUTHORIZED,
                        "The login credentials are invalid."))
                .thenReturn(STUB_401);

        var result = handler.handleSpringSecurityCredentialExceptions(ex, request);

        assertThat(result).isEqualTo(STUB_401);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when BadCredentialsException is thrown")
    void shouldReturnUnauthorizedWhenBadCredentialsExceptionThrown() {
        var ex = new BadCredentialsException("Bad credentials.");
        when(problemDetailFactory.build(
                        "invalid-credentials",
                        "Invalid credentials",
                        HttpStatus.UNAUTHORIZED,
                        "The login credentials are invalid."))
                .thenReturn(STUB_401);

        var result = handler.handleSpringSecurityCredentialExceptions(ex, request);

        assertThat(result).isEqualTo(STUB_401);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when DisabledException is thrown")
    void shouldReturnUnauthorizedWhenDisabledExceptionThrown() {
        var ex = new DisabledException("disabled");
        when(problemDetailFactory.build(
                        "auth-failed", "Authentication failed", HttpStatus.UNAUTHORIZED, "Authentication failed."))
                .thenReturn(STUB_401);

        var result = handler.handleSpringSecurityAccountStateExceptions(ex, request);

        assertThat(result).isEqualTo(STUB_401);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when AccountExpiredException is thrown")
    void shouldReturnUnauthorizedWhenAccountExpiredExceptionThrown() {
        var ex = new AccountExpiredException("expired");
        when(problemDetailFactory.build(
                        "auth-failed", "Authentication failed", HttpStatus.UNAUTHORIZED, "Authentication failed."))
                .thenReturn(STUB_401);

        var result = handler.handleSpringSecurityAccountStateExceptions(ex, request);

        assertThat(result).isEqualTo(STUB_401);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when CredentialsExpiredException is thrown")
    void shouldReturnUnauthorizedWhenCredentialsExpiredExceptionThrown() {
        var ex = new CredentialsExpiredException("expired");
        when(problemDetailFactory.build(
                        "auth-failed", "Authentication failed", HttpStatus.UNAUTHORIZED, "Authentication failed."))
                .thenReturn(STUB_401);

        var result = handler.handleSpringSecurityAccountStateExceptions(ex, request);

        assertThat(result).isEqualTo(STUB_401);
    }
}

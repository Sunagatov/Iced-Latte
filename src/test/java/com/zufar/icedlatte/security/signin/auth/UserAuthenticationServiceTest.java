package com.zufar.icedlatte.security.signin.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
import com.zufar.icedlatte.security.session.token.SessionTokenService;
import com.zufar.icedlatte.security.signin.exception.InvalidCredentialsException;
import com.zufar.icedlatte.security.signin.exception.UserAccountLockedException;
import com.zufar.icedlatte.security.signin.lockout.LoginAttemptService;
import com.zufar.icedlatte.security.signin.turnstile.TurnstileVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuthenticationService Tests")
class UserAuthenticationServiceTest {

    @InjectMocks
    private UserAuthenticationService userAuthenticationService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private SessionTokenService sessionTokenService;

    @Mock
    private TurnstileVerifier turnstileVerifier;

    @Mock
    private HttpServletRequest httpRequest;

    private final UserAuthenticationRequest request = mock(UserAuthenticationRequest.class);
    private final UserDetails userDetails = mock(UserDetails.class);

    @Test
    @DisplayName("Should return UserDetails when valid credentials are provided")
    void shouldReturnUserDetailsWhenValidCredentialsProvided() {
        Authentication authentication = mock(Authentication.class);
        when(request.getEmail()).thenReturn("  Known@Example.com ");
        when(request.getPassword()).thenReturn("password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        UserDetails result = userAuthenticationService.verifyCredentials(request);

        assertSame(userDetails, result);
        verify(authenticationManager)
                .authenticate(
                        argThat(authenticationToken -> "known@example.com".equals(authenticationToken.getPrincipal())
                                && "password".equals(authenticationToken.getCredentials())));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when invalid credentials are provided")
    void shouldThrowInvalidCredentialsExceptionWhenInvalidCredentialsProvided() {
        when(request.getEmail()).thenReturn("  Known@Example.com ");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> userAuthenticationService.verifyCredentials(request));

        verify(loginAttemptService).recordFailure("known@example.com");
        verify(authenticationManager)
                .authenticate(
                        argThat(authenticationToken -> "known@example.com".equals(authenticationToken.getPrincipal())));
    }

    @Test
    @DisplayName("Should throw UserAccountLockedException when user account is locked")
    void shouldThrowUserAccountLockedExceptionWhenUserAccountIsLocked() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("User account is locked"));

        assertThrows(UserAccountLockedException.class, () -> userAuthenticationService.verifyCredentials(request));

        verifyNoInteractions(loginAttemptService);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when user email is not found")
    void shouldThrowInvalidCredentialsExceptionWhenUserEmailIsNotFound() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new UsernameNotFoundException("missing"));

        assertThrows(InvalidCredentialsException.class, () -> userAuthenticationService.verifyCredentials(request));

        verifyNoInteractions(loginAttemptService);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when principal is not UserDetails")
    void shouldThrowInvalidCredentialsExceptionWhenPrincipalIsNotUserDetails() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn("plain-string-principal");

        assertThrows(InvalidCredentialsException.class, () -> userAuthenticationService.verifyCredentials(request));

        verifyNoInteractions(loginAttemptService);
    }

    @Test
    @DisplayName("Should rethrow unexpected authentication exceptions")
    void shouldRethrowUnexpectedAuthenticationExceptions() {
        AuthenticationException failure = new AuthenticationException("boom") {};
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(failure);

        AuthenticationException thrown =
                assertThrows(AuthenticationException.class, () -> userAuthenticationService.verifyCredentials(request));

        assertSame(failure, thrown);
        verifyNoInteractions(loginAttemptService);
    }

    @Test
    @DisplayName("Should verify Turnstile, credentials, and issue a session-bound token pair")
    void shouldAuthenticateAndIssueSessionBoundTokenPair() {
        Authentication authentication = mock(Authentication.class);
        AuthenticationTokens expectedResponse = new AuthenticationTokens("access-token", "refresh-token");

        when(request.getTurnstileToken()).thenReturn("turnstile-token");
        when(request.getEmail()).thenReturn("  Known@Example.com ");
        when(request.getPassword()).thenReturn("password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(sessionTokenService.issueForNewSession(userDetails, httpRequest)).thenReturn(expectedResponse);

        AuthenticationTokens response = userAuthenticationService.authenticate(request, httpRequest);

        assertSame(expectedResponse, response);
        verify(turnstileVerifier).verify("turnstile-token");
        verify(sessionTokenService).issueForNewSession(userDetails, httpRequest);
        verify(loginAttemptService).resetAfterSuccessfulAuthentication("known@example.com");
    }
}

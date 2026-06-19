package com.zufar.icedlatte.security.signup.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.session.management.AuthSessionRequestMetadata;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
import com.zufar.icedlatte.security.session.token.SessionTokenService;
import com.zufar.icedlatte.security.signin.exception.UserRegistrationException;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;
import com.zufar.icedlatte.user.api.UserRegistrationApi;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationService unit tests")
class UserRegistrationServiceTest {

    @Mock
    private UserRegistrationApi userRegistrationApi;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionTokenService sessionTokenService;

    @Mock
    private TurnstileVerifier turnstileVerifier;

    @InjectMocks
    private UserRegistrationService service;

    private static final AuthSessionRequestMetadata REQUEST_METADATA =
            new AuthSessionRequestMetadata("TestAgent", "127.0.0.1");

    @Test
    @DisplayName("register normalizes input, persists the user, and returns a session-bound token pair")
    void registerNormalizesInputPersistsUserAndReturnsTokenPair() {
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
        registrationRequest.setEmail("  Mixed.Case@Example.COM ");
        registrationRequest.setPassword("raw-password");
        registrationRequest.setTurnstileToken("turnstile-token");
        registrationRequest.setFirstName("Alice");
        registrationRequest.setLastName("Example");

        UserAuthenticationSnapshot snapshot = new UserAuthenticationSnapshot(
                UUID.randomUUID(),
                "mixed.case@example.com",
                "encoded-password",
                List.of("USER"),
                true,
                true,
                true,
                true);
        AuthenticationTokens tokenPair = new AuthenticationTokens("access-token", "refresh-token");

        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRegistrationApi.registerPasswordUser(any(), any(), any(), any()))
                .thenReturn(snapshot);
        when(sessionTokenService.issueForNewSession(any(), eq(REQUEST_METADATA)))
                .thenReturn(tokenPair);

        AuthenticationTokens response = service.register(registrationRequest, REQUEST_METADATA);

        verify(turnstileVerifier).verify("turnstile-token", "127.0.0.1");
        verify(userRegistrationApi).existsByEmail("mixed.case@example.com");
        verify(userRegistrationApi)
                .registerPasswordUser(eq("Alice"), eq("Example"), eq("mixed.case@example.com"), eq("encoded-password"));
        verify(sessionTokenService).issueForNewSession(any(), eq(REQUEST_METADATA));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("ensureRegistrationAllowed verifies Turnstile and normalized email availability")
    void ensureRegistrationAllowedVerifiesTurnstileAndEmailAvailability() {
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
        registrationRequest.setEmail("  Available@Example.COM ");
        registrationRequest.setTurnstileToken("turnstile-token");

        service.ensureRegistrationAllowed(registrationRequest);

        verify(turnstileVerifier).verify("turnstile-token", null);
        verify(userRegistrationApi).existsByEmail("available@example.com");
    }

    @Test
    @DisplayName("ensureRegistrationAllowed rejects existing normalized email")
    void ensureRegistrationAllowedRejectsExistingNormalizedEmail() {
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
        registrationRequest.setEmail("  Duplicate@Example.COM ");
        registrationRequest.setTurnstileToken("turnstile-token");
        when(userRegistrationApi.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.ensureRegistrationAllowed(registrationRequest))
                .isInstanceOf(UserRegistrationException.class)
                .hasMessage("This email is already registered. Please sign in or use a different email.");

        verify(turnstileVerifier).verify("turnstile-token", null);
        verify(userRegistrationApi).existsByEmail("duplicate@example.com");
    }

    @Test
    @DisplayName("register translates duplicate-email persistence failures")
    void registerTranslatesDuplicateEmailPersistenceFailures() {
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
        registrationRequest.setEmail("duplicate@example.com");
        registrationRequest.setPassword("raw-password");

        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRegistrationApi.registerPasswordUser(any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.register(registrationRequest, REQUEST_METADATA))
                .isInstanceOf(UserRegistrationException.class)
                .hasMessage("This email is already registered. Please sign in or use a different email.");

        verifyNoInteractions(sessionTokenService);
    }
}

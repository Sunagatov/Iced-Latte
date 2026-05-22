package com.zufar.icedlatte.security.service;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.session.token.SessionTokenService;
import com.zufar.icedlatte.security.signin.exception.UserRegistrationException;
import com.zufar.icedlatte.security.signup.registration.UserRegistrationService;
import com.zufar.icedlatte.user.api.UserAuthenticationSnapshot;
import com.zufar.icedlatte.user.api.UserRegistrationApi;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationService unit tests")
class UserRegistrationServiceTest {

    @Mock private UserRegistrationApi userRegistrationApi;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SessionTokenService sessionTokenService;
    @Mock private HttpServletRequest request;

    @InjectMocks private UserRegistrationService service;

    @Test
    @DisplayName("register normalizes input, persists the user, and returns a session-bound token pair")
    void registerNormalizesInputPersistsUserAndReturnsTokenPair() {
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
        registrationRequest.setEmail("  Mixed.Case@Example.COM ");
        registrationRequest.setPassword("raw-password");
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
                true
        );
        UserAuthenticationResponse tokenPair = new UserAuthenticationResponse();
        tokenPair.setToken("access-token");
        tokenPair.setRefreshToken("refresh-token");

        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRegistrationApi.registerPasswordUser(any(), any(), any(), any())).thenReturn(snapshot);
        when(sessionTokenService.issueForNewSession(any(), eq(request)))
                .thenReturn(tokenPair);

        UserAuthenticationResponse response = service.register(registrationRequest, request);

        verify(userRegistrationApi).registerPasswordUser(
                eq("Alice"),
                eq("Example"),
                eq("mixed.case@example.com"),
                eq("encoded-password")
        );
        verify(sessionTokenService).issueForNewSession(any(), eq(request));

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("ensureEmailAvailable rejects existing normalized email")
    void ensureEmailAvailableRejectsExistingNormalizedEmail() {
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
        registrationRequest.setEmail("  Duplicate@Example.COM ");
        when(userRegistrationApi.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.ensureEmailAvailable(registrationRequest))
                .isInstanceOf(UserRegistrationException.class)
                .hasMessage("This email is already registered. Please sign in or use a different email.");

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

        assertThatThrownBy(() -> service.register(registrationRequest, request))
                .isInstanceOf(UserRegistrationException.class)
                .hasMessage("This email is already registered. Please sign in or use a different email.");

        verifyNoInteractions(sessionTokenService);
    }
}

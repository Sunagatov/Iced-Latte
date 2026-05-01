package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.email.api.EmailTokenConformer;
import com.zufar.icedlatte.email.api.EmailTokenSender;
import com.zufar.icedlatte.openapi.dto.ChangePasswordRequest;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.ForgotPasswordRequest;
import com.zufar.icedlatte.openapi.dto.SessionInfo;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSecurityService unit tests")
class UserSecurityServiceTest {

    @Mock private UserAuthenticationService userAuthenticationService;
    @Mock private SessionTokenService sessionTokenService;
    @Mock private EmailTokenSender emailTokenSender;
    @Mock private EmailTokenConformer emailTokenConformer;
    @Mock private AuthSessionService authSessionService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private LogoutService logoutService;
    @Mock private PasswordResetService passwordResetService;
    @Mock private HttpServletRequest httpRequest;

    @InjectMocks private UserSecurityService service;

    @Test
    @DisplayName("requestRegistration delegates to email token sender")
    void requestRegistrationDelegatesToEmailTokenSender() {
        UserRegistrationRequest request = new UserRegistrationRequest();

        service.requestRegistration(request);

        verify(emailTokenSender).sendEmailVerificationCode(request);
    }

    @Test
    @DisplayName("confirmEmail delegates to email token conformer")
    void confirmEmailDelegatesToEmailTokenConformer() {
        ConfirmEmailRequest request = new ConfirmEmailRequest("token");
        UserAuthenticationResponse response = authResponse();
        when(emailTokenConformer.confirmEmailByCode(request, httpRequest)).thenReturn(response);

        UserAuthenticationResponse result = service.confirmEmail(request, httpRequest);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("authenticate verifies credentials and issues a managed session")
    void authenticateVerifiesCredentialsAndIssuesManagedSession() {
        UserAuthenticationRequest request = new UserAuthenticationRequest("alice@example.com", "secret");
        UserDetails userDetails = user();
        UserAuthenticationResponse response = authResponse();
        when(userAuthenticationService.verifyCredentials(request)).thenReturn(userDetails);
        when(sessionTokenService.issueForNewSession(userDetails, httpRequest)).thenReturn(response);

        UserAuthenticationResponse result = service.authenticate(request, httpRequest);

        assertThat(result).isSameAs(response);
        verify(userAuthenticationService).verifyCredentials(request);
        verify(sessionTokenService).issueForNewSession(userDetails, httpRequest);
    }

    @Test
    @DisplayName("refresh delegates to refresh token service")
    void refreshDelegatesToRefreshTokenService() {
        ResponseEntity<UserAuthenticationResponse> response =
                ResponseEntity.status(HttpStatus.CREATED).body(authResponse());
        when(refreshTokenService.refresh(httpRequest)).thenReturn(response);

        ResponseEntity<UserAuthenticationResponse> result = service.refresh(httpRequest);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("logout delegates to logout service")
    void logoutDelegatesToLogoutService() {
        service.logout("refresh-token", httpRequest);

        verify(logoutService).logout("refresh-token", httpRequest);
    }

    @Test
    @DisplayName("logoutAll delegates to logout service")
    void logoutAllDelegatesToLogoutService() {
        UUID userId = UUID.randomUUID();

        service.logoutAll(userId);

        verify(logoutService).logoutAll(userId);
    }

    @Nested
    @DisplayName("session management")
    class SessionManagement {

        @Test
        @DisplayName("getSessions maps active sessions to dto")
        void getSessionsMapsActiveSessionsToDto() {
            UUID userId = UUID.randomUUID();
            UUID sessionId = UUID.randomUUID();
            OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
            OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(1);
            OffsetDateTime lastUsedAt = OffsetDateTime.now().minusHours(1);
            AuthSessionEntity session = AuthSessionEntity.builder()
                    .id(sessionId)
                    .createdAt(createdAt)
                    .expiresAt(expiresAt)
                    .lastUsedAt(lastUsedAt)
                    .userAgent("Firefox")
                    .ipAddress("127.0.0.1")
                    .build();
            when(authSessionService.listActiveSessions(userId)).thenReturn(List.of(session));

            List<SessionInfo> result = service.getSessions(userId);

            assertThat(result).singleElement().satisfies(info -> {
                assertThat(info.getSessionId()).isEqualTo(sessionId);
                assertThat(info.getCreatedAt()).isEqualTo(createdAt);
                assertThat(info.getExpiresAt()).isEqualTo(expiresAt);
                assertThat(info.getLastUsedAt()).isEqualTo(lastUsedAt);
                assertThat(info.getUserAgent()).isEqualTo("Firefox");
                assertThat(info.getIpAddress()).isEqualTo("127.0.0.1");
            });
        }

        @Test
        @DisplayName("revokeSession delegates to auth session service")
        void revokeSessionDelegatesToAuthSessionService() {
            UUID sessionId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            service.revokeSession(sessionId, userId);

            verify(authSessionService).revokeById(sessionId, userId);
        }
    }

    @Test
    @DisplayName("requestPasswordReset delegates using the request email")
    void requestPasswordResetDelegatesUsingRequestEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@example.com");

        service.requestPasswordReset(request);

        verify(passwordResetService).requestReset("user@example.com");
    }

    @Test
    @DisplayName("confirmPasswordReset delegates using code and password")
    void confirmPasswordResetDelegatesUsingCodeAndPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("reset-code", "new-password");

        service.confirmPasswordReset(request);

        verify(passwordResetService).confirmReset("reset-code", "new-password");
    }

    private static UserDetails user() {
        return com.zufar.icedlatte.user.entity.UserEntity.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .password("secret")
                .build();
    }

    private static UserAuthenticationResponse authResponse() {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken("access-token");
        response.setRefreshToken("refresh-token");
        return response;
    }
}

package com.zufar.icedlatte.security.session.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import com.zufar.icedlatte.common.correlation.RequestContextConstants;
import com.zufar.icedlatte.security.jwt.blacklist.JwtTokenBlacklist;
import com.zufar.icedlatte.security.jwt.provider.JwtTokenProvider;
import com.zufar.icedlatte.security.session.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.session.management.AuthSessionService;
import com.zufar.icedlatte.security.signin.auth.SecurityUserDetails;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionTokenService unit tests")
class SessionTokenServiceTest {

    @Mock
    private JwtTokenBlacklist jwtTokenBlacklist;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private SessionTokenService service;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("issues a fresh managed session and clears MDC afterward")
    void issueForNewSessionCreatesManagedSession() {
        UUID userId = UUID.randomUUID();
        String refreshToken = "refresh-token";
        String refreshHash = "refresh-hash";
        String accessToken = "access-token";
        SecurityUserDetails user = user(userId, "alice@example.com");

        when(jwtTokenProvider.generateRefreshToken(eq(user), any(UUID.class))).thenReturn(refreshToken);
        when(jwtTokenProvider.generateToken(eq(user), any(UUID.class))).thenReturn(accessToken);
        when(jwtTokenBlacklist.hash(refreshToken)).thenReturn(refreshHash);
        when(authSessionService.createSession(any(UUID.class), eq(userId), eq(refreshHash), eq(request)))
                .thenAnswer(invocation -> AuthSessionEntity.builder()
                        .id(invocation.getArgument(0))
                        .userId(userId)
                        .build());

        AuthenticationTokens result = service.issueForNewSession(user, request);

        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(refreshToken);
        assertThat(MDC.get(RequestContextConstants.USER_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(RequestContextConstants.SESSION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("rotates session tokens and clears MDC afterward")
    void rotateSessionTokensRotatesAndClearsMdc() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String oldHash = "old-hash";
        String newRefreshToken = "new-refresh";
        String newHash = "new-hash";
        String accessToken = "access-token";
        SecurityUserDetails user = user(userId, "rotate@example.com");
        AuthSessionEntity session =
                AuthSessionEntity.builder().id(sessionId).userId(userId).build();

        when(jwtTokenProvider.generateRefreshToken(user, sessionId)).thenReturn(newRefreshToken);
        when(jwtTokenProvider.generateToken(user, sessionId)).thenReturn(accessToken);
        when(jwtTokenBlacklist.hash(newRefreshToken)).thenReturn(newHash);

        AuthenticationTokens result = service.rotateSessionTokens(session, oldHash, user);

        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);
        verify(authSessionService).rotateSession(session, oldHash, newHash);
        assertThat(MDC.get(RequestContextConstants.USER_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(RequestContextConstants.SESSION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("migrates a legacy refresh token into a managed session")
    void migrateLegacyRefreshTokenCreatesSessionAndBlacklistsLegacyToken() {
        UUID userId = UUID.randomUUID();
        String legacyToken = "legacy-token";
        String newRefreshToken = "new-refresh";
        String newHash = "new-hash";
        String accessToken = "access-token";
        SecurityUserDetails user = user(userId, "legacy@example.com");

        when(jwtTokenProvider.generateRefreshToken(eq(user), any(UUID.class))).thenReturn(newRefreshToken);
        when(jwtTokenProvider.generateToken(eq(user), any(UUID.class))).thenReturn(accessToken);
        when(jwtTokenBlacklist.hash(newRefreshToken)).thenReturn(newHash);
        when(authSessionService.createSession(any(UUID.class), eq(userId), eq(newHash), eq(request)))
                .thenAnswer(invocation -> AuthSessionEntity.builder()
                        .id(invocation.getArgument(0))
                        .userId(userId)
                        .build());

        AuthenticationTokens result = service.migrateLegacyRefreshToken(user, legacyToken, request);

        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);
        verify(jwtTokenBlacklist).blacklistRefreshToken(legacyToken);
        assertThat(MDC.get(RequestContextConstants.USER_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(RequestContextConstants.SESSION_ID_MDC_KEY)).isNull();
    }

    private static SecurityUserDetails user(UUID id, String email) {
        return new SecurityUserDetails(id, email, "secret", java.util.List.of(), true, true, true, true);
    }
}

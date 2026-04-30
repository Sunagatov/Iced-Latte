package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.common.correlation.RequestContextConstants;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.jwt.JwtBlacklistService;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionTokenService unit tests")
class SessionTokenServiceTest {

    @Mock private JwtBlacklistService jwtBlacklistService;
    @Mock private JwtBlacklistValidator jwtBlacklistValidator;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthSessionService authSessionService;
    @Mock private UserAuthenticationService userAuthenticationService;
    @Mock private HttpServletRequest request;

    @InjectMocks private SessionTokenService service;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("issues a fresh managed session and keeps MDC bound to the request")
    void issueForNewSessionCreatesManagedSession() {
        UUID userId = UUID.randomUUID();
        String refreshToken = "refresh-token";
        String refreshHash = "refresh-hash";
        UserEntity user = user(userId, "alice@example.com");
        UserAuthenticationResponse response = response(refreshToken);

        when(jwtTokenProvider.generateRefreshToken(eq(user), any(UUID.class))).thenReturn(refreshToken);
        when(jwtBlacklistService.sha256(refreshToken)).thenReturn(refreshHash);
        when(authSessionService.createSession(any(UUID.class), eq(userId), eq(refreshHash), eq(request)))
                .thenAnswer(invocation -> AuthSessionEntity.builder()
                        .id(invocation.getArgument(0))
                        .userId(userId)
                        .build());
        when(userAuthenticationService.buildTokenPair(eq(user), any(UUID.class), eq(refreshToken)))
                .thenReturn(response);

        UserAuthenticationResponse result = service.issueForNewSession(user, request);

        assertThat(result).isSameAs(response);
        assertThat(MDC.get(RequestContextConstants.USER_ID_MDC_KEY)).isEqualTo(userId.toString());
        assertThat(MDC.get(RequestContextConstants.SESSION_ID_MDC_KEY)).isNotBlank();
    }

    @Test
    @DisplayName("rotates session tokens and clears MDC afterward")
    void rotateSessionTokensRotatesAndClearsMdc() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String oldHash = "old-hash";
        String newRefreshToken = "new-refresh";
        String newHash = "new-hash";
        UserEntity user = user(userId, "rotate@example.com");
        AuthSessionEntity session = AuthSessionEntity.builder().id(sessionId).userId(userId).build();
        UserAuthenticationResponse response = response(newRefreshToken);

        when(jwtTokenProvider.generateRefreshToken(user, sessionId)).thenReturn(newRefreshToken);
        when(jwtBlacklistService.sha256(newRefreshToken)).thenReturn(newHash);
        when(userAuthenticationService.buildTokenPair(user, sessionId, newRefreshToken)).thenReturn(response);

        UserAuthenticationResponse result = service.rotateSessionTokens(session, oldHash, user);

        assertThat(result).isSameAs(response);
        verify(authSessionService).rotateSession(oldHash, newHash);
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
        UserEntity user = user(userId, "legacy@example.com");
        UserAuthenticationResponse response = response(newRefreshToken);

        when(jwtTokenProvider.generateRefreshToken(eq(user), any(UUID.class))).thenReturn(newRefreshToken);
        when(jwtBlacklistService.sha256(newRefreshToken)).thenReturn(newHash);
        when(authSessionService.createSession(any(UUID.class), eq(userId), eq(newHash), eq(request)))
                .thenAnswer(invocation -> AuthSessionEntity.builder()
                        .id(invocation.getArgument(0))
                        .userId(userId)
                        .build());
        when(userAuthenticationService.buildTokenPair(eq(user), any(UUID.class), eq(newRefreshToken)))
                .thenReturn(response);

        UserAuthenticationResponse result = service.migrateLegacyRefreshToken(user, legacyToken, request);

        assertThat(result).isSameAs(response);
        verify(jwtBlacklistValidator).addToBlacklist(legacyToken);
        assertThat(MDC.get(RequestContextConstants.USER_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(RequestContextConstants.SESSION_ID_MDC_KEY)).isNull();
    }

    private static UserEntity user(UUID id, String email) {
        return UserEntity.builder()
                .id(id)
                .email(email)
                .password("secret")
                .build();
    }

    private static UserAuthenticationResponse response(String refreshToken) {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken("access-token");
        response.setRefreshToken(refreshToken);
        return response;
    }
}

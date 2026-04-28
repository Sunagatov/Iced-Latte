package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.jwt.JwtBlacklistService;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtRefreshTokenValidator;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService unit tests")
class RefreshTokenServiceTest {

    @Mock private JwtRefreshTokenValidator jwtRefreshTokenValidator;
    @Mock private JwtBlacklistService jwtBlacklistService;
    @Mock private JwtBlacklistValidator jwtBlacklistValidator;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserDetailsService userDetailsService;
    @Mock private AuthSessionService authSessionService;
    @Mock private UserAuthenticationService userAuthenticationService;
    @Mock private HttpServletRequest request;

    @InjectMocks private RefreshTokenService service;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("rotates an active managed session and returns a fresh token pair")
        void rotatesActiveManagedSessionAndReturnsFreshTokenPair() {
            String rawToken = "raw-refresh-token";
            String oldHash = "old-hash";
            String newRefreshToken = "new-refresh-token";
            String newHash = "new-hash";
            String email = "alice@example.com";
            UUID sessionId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            UserEntity user = user(userId, email);
            AuthSessionEntity session = AuthSessionEntity.builder().id(sessionId).userId(userId).build();
            UserAuthenticationResponse responseBody = response(newRefreshToken);

            when(jwtRefreshTokenValidator.extractRawToken(request)).thenReturn(rawToken);
            when(jwtBlacklistService.sha256(rawToken)).thenReturn(oldHash);
            when(authSessionService.findActiveByHash(oldHash)).thenReturn(session);
            when(jwtRefreshTokenValidator.extractEmail(request)).thenReturn(email);
            when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
            when(jwtTokenProvider.generateRefreshToken(user, sessionId)).thenReturn(newRefreshToken);
            when(jwtBlacklistService.sha256(newRefreshToken)).thenReturn(newHash);
            when(userAuthenticationService.buildTokenPair(user, email, sessionId, newRefreshToken))
                    .thenReturn(responseBody);

            ResponseEntity<UserAuthenticationResponse> response = service.refresh(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(responseBody);
            verify(authSessionService).rotateSession(oldHash, newHash);
            assertThat(MDC.get("sessionId")).isNull();
            assertThat(MDC.get("userId")).isNull();
        }

        @Test
        @DisplayName("migrates a legacy refresh token into a managed session")
        void migratesLegacyRefreshTokenIntoManagedSession() {
            String rawToken = "legacy-refresh-token";
            String oldHash = "legacy-hash";
            String email = "legacy@example.com";
            UUID userId = UUID.randomUUID();
            UUID newSessionId = UUID.randomUUID();
            String newRefreshToken = "new-refresh-token";
            String newHash = "new-hash";

            UserEntity user = user(userId, email);
            AuthSessionEntity newSession = AuthSessionEntity.builder().id(newSessionId).userId(userId).build();
            UserAuthenticationResponse responseBody = response(newRefreshToken);

            when(jwtRefreshTokenValidator.extractRawToken(request)).thenReturn(rawToken);
            when(jwtBlacklistService.sha256(rawToken)).thenReturn(oldHash);
            when(authSessionService.findActiveByHash(oldHash))
                    .thenThrow(new JwtTokenBlacklistedException("Refresh token not found"));
            when(jwtRefreshTokenValidator.isSessionManaged(rawToken)).thenReturn(false);
            when(jwtRefreshTokenValidator.extractEmail(request)).thenReturn(email);
            when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
            when(jwtTokenProvider.generateRefreshToken(eq(user), any(UUID.class))).thenReturn(newRefreshToken);
            when(jwtBlacklistService.sha256(newRefreshToken)).thenReturn(newHash);
            when(authSessionService.createSession(any(UUID.class), eq(userId), eq(newHash), eq(request)))
                    .thenReturn(newSession);
            when(userAuthenticationService.buildTokenPair(user, email, newSessionId, newRefreshToken))
                    .thenReturn(responseBody);

            ResponseEntity<UserAuthenticationResponse> response = service.refresh(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isSameAs(responseBody);
            verify(jwtBlacklistValidator).addToBlacklist(rawToken);

            ArgumentCaptor<UUID> generatedSessionId = ArgumentCaptor.forClass(UUID.class);
            verify(jwtTokenProvider).generateRefreshToken(eq(user), generatedSessionId.capture());
            verify(authSessionService).createSession(eq(generatedSessionId.getValue()), eq(userId), eq(newHash), eq(request));
            assertThat(MDC.get("sessionId")).isNull();
            assertThat(MDC.get("userId")).isNull();
        }

        @Test
        @DisplayName("revokes all sessions when a managed replayed token is detected")
        void revokesAllSessionsWhenManagedReplayedTokenIsDetected() {
            String rawToken = "managed-refresh-token";
            String hash = "managed-hash";
            UUID sessionId = UUID.randomUUID();
            JwtTokenBlacklistedException failure = new JwtTokenBlacklistedException("Refresh token rotated");

            when(jwtRefreshTokenValidator.extractRawToken(request)).thenReturn(rawToken);
            when(jwtBlacklistService.sha256(rawToken)).thenReturn(hash);
            when(authSessionService.findActiveByHash(hash)).thenThrow(failure);
            when(jwtRefreshTokenValidator.isSessionManaged(rawToken)).thenReturn(true);
            when(jwtRefreshTokenValidator.extractSessionId(rawToken)).thenReturn(Optional.of(sessionId));

            assertThatThrownBy(() -> service.refresh(request))
                    .isSameAs(failure);

            verify(authSessionService).revokeAllForUserBySessionId(sessionId);
            verifyNoInteractions(userDetailsService, userAuthenticationService);
            assertThat(MDC.get("sessionId")).isNull();
            assertThat(MDC.get("userId")).isNull();
        }
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

package com.zufar.icedlatte.security.service;

import com.zufar.icedlatte.security.service.session.AuthSessionService;
import com.zufar.icedlatte.security.service.session.SessionTokenService;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.jwt.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.JwtTokenBlacklist;
import com.zufar.icedlatte.security.jwt.JwtTokenClaims;
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
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService unit tests")
class RefreshTokenServiceTest {

    @Mock private JwtBearerTokenResolver jwtBearerTokenResolver;
    @Mock private JwtTokenClaims jwtTokenClaims;
    @Mock private JwtTokenBlacklist jwtTokenBlacklist;
    @Mock private UserDetailsService userDetailsService;
    @Mock private AuthSessionService authSessionService;
    @Mock private SessionTokenService sessionTokenService;
    @Mock private HttpServletRequest request;

    @InjectMocks private RefreshTokenService service;

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("rotates an active managed session and returns a fresh token pair")
        void rotatesActiveManagedSessionAndReturnsFreshTokenPair() {
            String rawToken = "raw-refresh-token";
            String oldHash = "old-hash";
            String email = "alice@example.com";
            UUID userId = UUID.randomUUID();
            var user = user(email);
            var sessionId = UUID.randomUUID();
            AuthSessionEntity session = AuthSessionEntity.builder().id(sessionId).userId(userId).build();
            UserAuthenticationResponse responseBody = response();

            when(jwtBearerTokenResolver.extract(request)).thenReturn(rawToken);
            when(jwtTokenBlacklist.hash(rawToken)).thenReturn(oldHash);
            when(authSessionService.findActiveByHash(oldHash)).thenReturn(session);
            when(jwtTokenClaims.extractRefreshTokenEmail(rawToken)).thenReturn(email);
            when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
            when(sessionTokenService.rotateSessionTokens(session, oldHash, user)).thenReturn(responseBody);

            ResponseEntity<UserAuthenticationResponse> response = service.refresh(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isSameAs(responseBody);
            verify(sessionTokenService).rotateSessionTokens(session, oldHash, user);
        }

        @Test
        @DisplayName("migrates a legacy refresh token into a managed session")
        void migratesLegacyRefreshTokenIntoManagedSession() {
            String rawToken = "legacy-refresh-token";
            String oldHash = "legacy-hash";
            String email = "legacy@example.com";
            var user = user(email);
            UserAuthenticationResponse responseBody = response();

            when(jwtBearerTokenResolver.extract(request)).thenReturn(rawToken);
            when(jwtTokenBlacklist.hash(rawToken)).thenReturn(oldHash);
            when(authSessionService.findActiveByHash(oldHash))
                    .thenThrow(new JwtTokenBlacklistedException("Refresh token not found"));
            when(jwtTokenClaims.isSessionManagedRefreshToken(rawToken)).thenReturn(false);
            when(jwtTokenClaims.extractRefreshTokenEmail(rawToken)).thenReturn(email);
            when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
            when(sessionTokenService.migrateLegacyRefreshToken(user, rawToken, request)).thenReturn(responseBody);

            ResponseEntity<UserAuthenticationResponse> response = service.refresh(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isSameAs(responseBody);
            verify(sessionTokenService).migrateLegacyRefreshToken(user, rawToken, request);
        }

        @Test
        @DisplayName("revokes all sessions when a managed replayed token is detected")
        void revokesAllSessionsWhenManagedReplayedTokenIsDetected() {
            String rawToken = "managed-refresh-token";
            String hash = "managed-hash";
            UUID sessionId = UUID.randomUUID();
            JwtTokenBlacklistedException failure = new JwtTokenBlacklistedException("Refresh token rotated");

            when(jwtBearerTokenResolver.extract(request)).thenReturn(rawToken);
            when(jwtTokenBlacklist.hash(rawToken)).thenReturn(hash);
            when(authSessionService.findActiveByHash(hash)).thenThrow(failure);
            when(jwtTokenClaims.isSessionManagedRefreshToken(rawToken)).thenReturn(true);
            when(jwtTokenClaims.extractRefreshTokenSessionId(rawToken)).thenReturn(Optional.of(sessionId));

            assertThatThrownBy(() -> service.refresh(request))
                    .isSameAs(failure);

            verify(authSessionService).revokeAllForUserBySessionId(sessionId);
            verifyNoInteractions(userDetailsService, sessionTokenService);
        }
    }

    private static org.springframework.security.core.userdetails.UserDetails user(String email) {
        return com.zufar.icedlatte.user.entity.UserEntity.builder()
                .id(java.util.UUID.randomUUID())
                .email(email)
                .password("secret")
                .build();
    }

    private static UserAuthenticationResponse response() {
        UserAuthenticationResponse response = new UserAuthenticationResponse();
        response.setToken("access-token");
        response.setRefreshToken("new-refresh-token");
        return response;
    }
}

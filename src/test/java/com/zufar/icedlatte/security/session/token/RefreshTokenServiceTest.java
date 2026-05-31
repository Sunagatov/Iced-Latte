package com.zufar.icedlatte.security.session.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.zufar.icedlatte.security.jwt.blacklist.JwtTokenBlacklist;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.jwt.provider.JwtAccountStatusValidator;
import com.zufar.icedlatte.security.jwt.resolver.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.resolver.JwtTokenClaims;
import com.zufar.icedlatte.security.session.entity.AuthSessionEntity;
import com.zufar.icedlatte.security.session.management.AuthSessionService;
import com.zufar.icedlatte.security.signin.auth.SecurityUserDetails;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService unit tests")
class RefreshTokenServiceTest {

    @Mock
    private JwtBearerTokenResolver jwtBearerTokenResolver;

    @Mock
    private JwtTokenClaims jwtTokenClaims;

    @Mock
    private JwtTokenBlacklist jwtTokenBlacklist;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private SessionTokenService sessionTokenService;

    @Mock
    private JwtAccountStatusValidator jwtAccountStatusValidator;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private RefreshTokenService service;

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
            AuthSessionEntity session =
                    AuthSessionEntity.builder().id(sessionId).userId(userId).build();
            AuthenticationTokens responseBody = response();

            when(jwtBearerTokenResolver.extract(request)).thenReturn(rawToken);
            when(jwtTokenBlacklist.hash(rawToken)).thenReturn(oldHash);
            when(authSessionService.findActiveByHash(oldHash)).thenReturn(session);
            when(jwtTokenClaims.extractRefreshTokenEmail(rawToken)).thenReturn(email);
            when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
            when(sessionTokenService.rotateSessionTokens(session, oldHash, user))
                    .thenReturn(responseBody);

            RefreshTokenResult response = service.refresh(request);

            assertThat(response.migratedLegacyToken()).isFalse();
            assertThat(response.tokens()).isSameAs(responseBody);
            verify(jwtAccountStatusValidator).requireActive(user);
            verify(sessionTokenService).rotateSessionTokens(session, oldHash, user);
        }

        @Test
        @DisplayName("migrates a legacy refresh token into a managed session")
        void migratesLegacyRefreshTokenIntoManagedSession() {
            String rawToken = "legacy-refresh-token";
            String oldHash = "legacy-hash";
            String email = "legacy@example.com";
            var user = user(email);
            AuthenticationTokens responseBody = response();

            when(jwtBearerTokenResolver.extract(request)).thenReturn(rawToken);
            when(jwtTokenBlacklist.hash(rawToken)).thenReturn(oldHash);
            when(authSessionService.findActiveByHash(oldHash))
                    .thenThrow(new JwtTokenBlacklistedException("Refresh token not found"));
            when(jwtTokenClaims.isSessionManagedRefreshToken(rawToken)).thenReturn(false);
            when(jwtTokenClaims.extractRefreshTokenEmail(rawToken)).thenReturn(email);
            when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
            when(sessionTokenService.migrateLegacyRefreshToken(user, rawToken, request))
                    .thenReturn(responseBody);

            RefreshTokenResult response = service.refresh(request);

            assertThat(response.migratedLegacyToken()).isTrue();
            assertThat(response.tokens()).isSameAs(responseBody);
            verify(jwtAccountStatusValidator).requireActive(user);
            verify(sessionTokenService).migrateLegacyRefreshToken(user, rawToken, request);
        }

        @Test
        @DisplayName("rejects managed refresh when loaded user account is inactive")
        void rejectsManagedRefreshWhenLoadedUserAccountIsInactive() {
            String rawToken = "raw-refresh-token";
            String oldHash = "old-hash";
            String email = "locked@example.com";
            var user = user(email);
            AuthSessionEntity session = AuthSessionEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(UUID.randomUUID())
                    .build();
            JwtTokenBlacklistedException failure = new JwtTokenBlacklistedException("User account is not active");

            when(jwtBearerTokenResolver.extract(request)).thenReturn(rawToken);
            when(jwtTokenBlacklist.hash(rawToken)).thenReturn(oldHash);
            when(authSessionService.findActiveByHash(oldHash)).thenReturn(session);
            when(jwtTokenClaims.extractRefreshTokenEmail(rawToken)).thenReturn(email);
            when(userDetailsService.loadUserByUsername(email)).thenReturn(user);
            doThrow(failure).when(jwtAccountStatusValidator).requireActive(user);

            assertThatThrownBy(() -> service.refresh(request)).isSameAs(failure);

            verifyNoInteractions(sessionTokenService);
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

            assertThatThrownBy(() -> service.refresh(request)).isSameAs(failure);

            verify(authSessionService).revokeAllForCompromisedUserBySessionId(sessionId);
            verifyNoInteractions(userDetailsService, sessionTokenService);
        }
    }

    private static org.springframework.security.core.userdetails.UserDetails user(String email) {
        return new SecurityUserDetails(
                java.util.UUID.randomUUID(), email, "secret", java.util.List.of(), true, true, true, true);
    }

    private static AuthenticationTokens response() {
        return new AuthenticationTokens("access-token", "new-refresh-token");
    }
}

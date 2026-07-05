package com.zufar.icedlatte.security.session.revocation;

import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.security.jwt.blacklist.JwtTokenBlacklist;
import com.zufar.icedlatte.security.jwt.resolver.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.resolver.JwtTokenClaims;
import com.zufar.icedlatte.security.session.management.AuthSessionService;
import com.zufar.icedlatte.security.signin.exception.AbsentBearerHeaderException;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRevocationService unit tests")
class TokenRevocationServiceTest {

    @Mock
    private JwtTokenBlacklist jwtTokenBlacklist;

    @Mock
    private JwtBearerTokenResolver jwtBearerTokenResolver;

    @Mock
    private JwtTokenClaims jwtTokenClaims;

    @Mock
    private AuthSessionService authSessionService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private TokenRevocationService service;

    @Nested
    @DisplayName("revokeTokens")
    class RevokeTokens {

        @Test
        @DisplayName("revokes and blacklists refresh and access tokens when both are present")
        void revokesAndBlacklistsRefreshAndAccessTokens() {
            when(jwtBearerTokenResolver.extract(request)).thenReturn("access.header.payload");
            when(jwtTokenBlacklist.hash("refresh-token")).thenReturn("refresh-hash");

            service.revokeTokens("refresh-token", request);

            verify(authSessionService).revokeByRefreshTokenHash("refresh-hash");
            verify(jwtTokenBlacklist).blacklistRefreshToken("refresh-token");
            verify(jwtTokenBlacklist).blacklist("access.header.payload");
        }

        @Test
        @DisplayName("revokes the backing session from the access token when refresh header is absent")
        void revokesBackingSessionFromAccessTokenWhenRefreshHeaderAbsent() {
            var sessionId = java.util.UUID.randomUUID();
            when(jwtBearerTokenResolver.extract(request)).thenReturn("access-token");
            when(jwtTokenClaims.extractAccessTokenSessionId("access-token"))
                    .thenReturn(java.util.Optional.of(sessionId));

            service.revokeTokens("  ", request);

            verify(authSessionService).revokeBySessionId(sessionId);
            verify(jwtTokenBlacklist).blacklist("access-token");
            verify(jwtTokenBlacklist, never()).blacklistRefreshToken(any());
        }

        @Test
        @DisplayName("still blacklists the access token when refresh token extraction fails")
        void stillBlacklistsAccessTokenWhenRefreshTokenExtractionFails() {
            doThrow(new AbsentBearerHeaderException("missing"))
                    .when(jwtBearerTokenResolver)
                    .extract(request);

            service.revokeTokens(null, request);

            verify(jwtBearerTokenResolver).extract(request);
            verifyNoInteractions(authSessionService);
        }

        @Test
        @DisplayName("swallows malformed authorization headers")
        void swallowsMalformedAuthorizationHeaders() {
            when(jwtBearerTokenResolver.extract(request)).thenReturn("access-token");
            when(jwtTokenBlacklist.hash("refresh-token")).thenReturn("refresh-hash");

            service.revokeTokens("refresh-token", request);

            verify(authSessionService).revokeByRefreshTokenHash("refresh-hash");
            verify(jwtTokenBlacklist).blacklistRefreshToken("refresh-token");
            verify(jwtTokenBlacklist).blacklist("access-token");
        }

        @Test
        @DisplayName("uses configured request token extraction instead of a hardcoded authorization header")
        void usesConfiguredRequestTokenExtractionInsteadOfHardcodedAuthorizationHeader() {
            when(jwtBearerTokenResolver.extract(request)).thenReturn("custom-header-access-token");

            service.revokeTokens(null, request);

            verify(jwtBearerTokenResolver).extract(request);
            verify(jwtTokenBlacklist).blacklist("custom-header-access-token");
        }
    }
}

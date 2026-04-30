package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import com.zufar.icedlatte.security.jwt.JwtBlacklistService;
import com.zufar.icedlatte.security.jwt.JwtBlacklistValidator;
import com.zufar.icedlatte.security.jwt.JwtRefreshTokenValidator;
import com.zufar.icedlatte.security.jwt.JwtTokenFromAuthHeaderExtractor;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRevocationService unit tests")
class TokenRevocationServiceTest {

    @Mock private JwtRefreshTokenValidator jwtRefreshTokenValidator;
    @Mock private JwtBlacklistService jwtBlacklistService;
    @Mock private JwtBlacklistValidator jwtBlacklistValidator;
    @Mock private JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    @Mock private AuthSessionService authSessionService;
    @Mock private HttpServletRequest request;

    @InjectMocks private TokenRevocationService service;

    @Nested
    @DisplayName("revokeTokens")
    class RevokeTokens {

        @Test
        @DisplayName("revokes and blacklists refresh and access tokens when both are present")
        void revokesAndBlacklistsRefreshAndAccessTokens() {
            when(request.getHeader("Authorization")).thenReturn("Bearer access.header.payload");
            when(jwtBlacklistService.sha256("refresh-token")).thenReturn("refresh-hash");
            when(jwtTokenFromAuthHeaderExtractor.extract("Bearer access.header.payload"))
                    .thenReturn("access.header.payload");

            service.revokeTokens("refresh-token", request);

            verify(authSessionService).revokeByRefreshTokenHash("refresh-hash");
            verify(jwtBlacklistValidator).addToBlacklist("refresh-token");
            verify(jwtBlacklistValidator).addToBlacklist("access.header.payload");
        }

        @Test
        @DisplayName("falls back to the request refresh token when header argument is blank")
        void fallsBackToRefreshTokenFromRequest() {
            when(jwtRefreshTokenValidator.extractRawToken(request)).thenReturn("refresh-from-request");
            when(jwtBlacklistService.sha256("refresh-from-request")).thenReturn("refresh-hash");

            service.revokeTokens("  ", request);

            verify(jwtRefreshTokenValidator).extractRawToken(request);
            verify(authSessionService).revokeByRefreshTokenHash("refresh-hash");
            verify(jwtBlacklistValidator).addToBlacklist("refresh-from-request");
        }

        @Test
        @DisplayName("still blacklists the access token when refresh token extraction fails")
        void stillBlacklistsAccessTokenWhenRefreshTokenExtractionFails() {
            when(request.getHeader("Authorization")).thenReturn("Bearer access.header.payload");
            doThrow(new AbsentBearerHeaderException("missing"))
                    .when(jwtRefreshTokenValidator).extractRawToken(request);
            when(jwtTokenFromAuthHeaderExtractor.extract("Bearer access.header.payload"))
                    .thenReturn("access.header.payload");

            service.revokeTokens(null, request);

            verify(jwtRefreshTokenValidator).extractRawToken(request);
            verify(jwtBlacklistValidator).addToBlacklist("access.header.payload");
            verifyNoInteractions(authSessionService);
        }

        @Test
        @DisplayName("swallows malformed authorization headers")
        void swallowsMalformedAuthorizationHeaders() {
            when(request.getHeader("Authorization")).thenReturn("Broken");
            when(jwtBlacklistService.sha256("refresh-token")).thenReturn("refresh-hash");
            doThrow(new AbsentBearerHeaderException("invalid"))
                    .when(jwtTokenFromAuthHeaderExtractor).extract("Broken");

            service.revokeTokens("refresh-token", request);

            verify(authSessionService).revokeByRefreshTokenHash("refresh-hash");
            verify(jwtBlacklistValidator).addToBlacklist("refresh-token");
        }
    }
}

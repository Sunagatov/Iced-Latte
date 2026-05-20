package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import com.zufar.icedlatte.security.jwt.support.JwtSigningKeys;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JwtTokenClaims unit tests")
class JwtTokenClaimsTest {

    private SecretKey accessKey;
    private SecretKey refreshKey;
    private JwtTokenClaims claims;

    @BeforeEach
    void setUp() {
        accessKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(new byte[64]);
        refreshKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(new byte[64]);
        JwtSigningKeys signingKeys = mock(JwtSigningKeys.class);
        when(signingKeys.get()).thenReturn(accessKey);
        when(signingKeys.getRefresh()).thenReturn(refreshKey);
        claims = new JwtTokenClaims(signingKeys);
    }

    @Test
    @DisplayName("extractAccessTokenEmail returns subject from valid token")
    void extractAccessTokenEmailReturnsSubject() {
        assertThat(claims.extractAccessTokenEmail(buildToken(accessKey, null, false)))
                .isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("extractAccessTokenEmail throws for invalid token")
    void extractAccessTokenEmailThrowsForInvalidToken() {
        assertThatThrownBy(() -> claims.extractAccessTokenEmail("not.a.token"))
                .isInstanceOf(JwtTokenHasNoUserEmailException.class);
    }

    @Test
    @DisplayName("extractAccessTokenSessionId returns UUID when sid claim is present")
    void extractAccessTokenSessionIdReturnsSid() {
        UUID sessionId = UUID.randomUUID();

        assertThat(claims.extractAccessTokenSessionId(buildToken(accessKey, sessionId.toString(), false)))
                .isEqualTo(Optional.of(sessionId));
    }

    @Test
    @DisplayName("extractAccessTokenSessionId returns empty for invalid token")
    void extractAccessTokenSessionIdReturnsEmptyForInvalidToken() {
        assertThat(claims.extractAccessTokenSessionId("bad.token")).isEmpty();
    }

    @Test
    @DisplayName("extractRefreshTokenEmail returns subject from valid refresh token")
    void extractRefreshTokenEmailReturnsSubject() {
        assertThat(claims.extractRefreshTokenEmail(buildToken(refreshKey, null, true)))
                .isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("isSessionManagedRefreshToken returns true when ver claim is present")
    void isSessionManagedRefreshTokenReturnsTrueWhenVersionClaimIsPresent() {
        assertThat(claims.isSessionManagedRefreshToken(buildToken(refreshKey, null, true))).isTrue();
    }

    @Test
    @DisplayName("isSessionManagedRefreshToken returns false when ver claim is absent")
    void isSessionManagedRefreshTokenReturnsFalseWhenVersionClaimIsAbsent() {
        assertThat(claims.isSessionManagedRefreshToken(buildToken(refreshKey, null, false))).isFalse();
    }

    @Test
    @DisplayName("extractRefreshTokenSessionId returns UUID when sid claim is present")
    void extractRefreshTokenSessionIdReturnsSid() {
        UUID sessionId = UUID.randomUUID();

        assertThat(claims.extractRefreshTokenSessionId(buildToken(refreshKey, sessionId.toString(), true)))
                .isEqualTo(Optional.of(sessionId));
    }

    private String buildToken(SecretKey key, String sessionId, boolean includeVersion) {
        var builder = Jwts.builder()
                .subject("user@example.com")
                .expiration(new Date(System.currentTimeMillis() + 60_000));
        if (sessionId != null) {
            builder.claim("sid", sessionId);
        }
        if (includeVersion) {
            builder.claim("ver", 2);
        }
        return builder.signWith(key).compact();
    }
}

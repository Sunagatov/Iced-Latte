package com.zufar.icedlatte.security.jwt.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.security.jwt.config.JwtClaimNames;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.jwt.config.JwtSigningKeys;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

@DisplayName("JwtTokenClaims unit tests")
class JwtTokenClaimsTest {

    private static final byte[] ACCESS_BYTES = new byte[64];
    private static final byte[] REFRESH_BYTES = createBytes(1);
    private static final byte[] PREVIOUS_ACCESS_BYTES = createBytes(2);
    private static final byte[] PREVIOUS_REFRESH_BYTES = createBytes(3);
    private SecretKey accessKey;
    private SecretKey refreshKey;
    private SecretKey previousAccessKey;
    private SecretKey previousRefreshKey;
    private JwtTokenClaims claims;

    private static final String ISSUER = "iced-latte";
    private static final String AUDIENCE = "iced-latte-client";

    @BeforeEach
    void setUp() {
        accessKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(ACCESS_BYTES);
        refreshKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(REFRESH_BYTES);
        previousAccessKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(PREVIOUS_ACCESS_BYTES);
        previousRefreshKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(PREVIOUS_REFRESH_BYTES);
        JwtProperties jwtProperties = new JwtProperties(
                "Authorization",
                java.util.Base64.getEncoder().encodeToString(ACCESS_BYTES),
                java.util.Base64.getEncoder().encodeToString(REFRESH_BYTES),
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                ISSUER,
                AUDIENCE,
                "access-active",
                "refresh-active",
                java.util.Base64.getEncoder().encodeToString(PREVIOUS_ACCESS_BYTES),
                java.util.Base64.getEncoder().encodeToString(PREVIOUS_REFRESH_BYTES),
                "access-previous",
                "refresh-previous");
        claims = new JwtTokenClaims(new JwtSigningKeys(jwtProperties), jwtProperties);
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
        assertThatThrownBy(() -> claims.extractAccessTokenEmail("not.a.token")).isInstanceOf(JwtTokenException.class);
    }

    @Test
    @DisplayName("extractAccessTokenEmail preserves expired errors for legacy tokens signed with the current key")
    void extractAccessTokenEmailPreservesExpiredErrorsForLegacyCurrentKeyToken() {
        assertThatThrownBy(() -> claims.extractAccessTokenEmail(buildExpiredToken(accessKey, null)))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("extractAccessTokenEmail throws when issuer does not match")
    void extractAccessTokenEmailThrowsWhenIssuerDoesNotMatch() {
        assertThatThrownBy(() ->
                        claims.extractAccessTokenEmail(buildToken(accessKey, null, false, "wrong-issuer", AUDIENCE)))
                .isInstanceOf(JwtTokenException.class);
    }

    @Test
    @DisplayName("extractAccessTokenEmail throws when audience does not match")
    void extractAccessTokenEmailThrowsWhenAudienceDoesNotMatch() {
        assertThatThrownBy(() ->
                        claims.extractAccessTokenEmail(buildToken(accessKey, null, false, ISSUER, "wrong-audience")))
                .isInstanceOf(JwtTokenException.class);
    }

    @Test
    @DisplayName("extractAccessTokenEmail throws when token purpose is refresh")
    void extractAccessTokenEmailThrowsWhenPurposeIsRefresh() {
        assertThatThrownBy(() -> claims.extractAccessTokenEmail(buildToken(
                        accessKey, null, null, false, ISSUER, AUDIENCE, JwtClaimNames.REFRESH_TOKEN_PURPOSE)))
                .isInstanceOf(JwtTokenException.class);
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
    @DisplayName("extractRefreshTokenEmail throws when issuer does not match")
    void extractRefreshTokenEmailThrowsWhenIssuerDoesNotMatch() {
        assertThatThrownBy(() ->
                        claims.extractRefreshTokenEmail(buildToken(refreshKey, null, true, "wrong-issuer", AUDIENCE)))
                .isInstanceOf(JwtTokenException.class);
    }

    @Test
    @DisplayName("extractRefreshTokenEmail throws when audience does not match")
    void extractRefreshTokenEmailThrowsWhenAudienceDoesNotMatch() {
        assertThatThrownBy(() ->
                        claims.extractRefreshTokenEmail(buildToken(refreshKey, null, true, ISSUER, "wrong-audience")))
                .isInstanceOf(JwtTokenException.class);
    }

    @Test
    @DisplayName("extractRefreshTokenEmail throws when token purpose is access")
    void extractRefreshTokenEmailThrowsWhenPurposeIsAccess() {
        assertThatThrownBy(() -> claims.extractRefreshTokenEmail(
                        buildToken(refreshKey, null, null, true, ISSUER, AUDIENCE, JwtClaimNames.ACCESS_TOKEN_PURPOSE)))
                .isInstanceOf(JwtTokenException.class);
    }

    @Test
    @DisplayName("isSessionManagedRefreshToken returns true when ver claim is present")
    void isSessionManagedRefreshTokenReturnsTrueWhenVersionClaimIsPresent() {
        assertThat(claims.isSessionManagedRefreshToken(buildToken(refreshKey, null, true)))
                .isTrue();
    }

    @Test
    @DisplayName("isSessionManagedRefreshToken returns false when ver claim is absent")
    void isSessionManagedRefreshTokenReturnsFalseWhenVersionClaimIsAbsent() {
        assertThat(claims.isSessionManagedRefreshToken(buildToken(refreshKey, null, false)))
                .isFalse();
    }

    @Test
    @DisplayName("extractRefreshTokenSessionId returns UUID when sid claim is present")
    void extractRefreshTokenSessionIdReturnsSid() {
        UUID sessionId = UUID.randomUUID();

        assertThat(claims.extractRefreshTokenSessionId(buildToken(refreshKey, sessionId.toString(), true)))
                .isEqualTo(Optional.of(sessionId));
    }

    @Test
    @DisplayName("extractAccessTokenEmail accepts tokens signed with the previous access key id")
    void extractAccessTokenEmailAcceptsPreviousAccessKey() {
        assertThat(claims.extractAccessTokenEmail(buildToken(previousAccessKey, "access-previous", null, false)))
                .isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("extractRefreshTokenEmail accepts tokens signed with the previous refresh key id")
    void extractRefreshTokenEmailAcceptsPreviousRefreshKey() {
        assertThat(claims.extractRefreshTokenEmail(buildToken(previousRefreshKey, "refresh-previous", null, true)))
                .isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("extractAccessTokenEmail accepts legacy tokens without a kid using the current key")
    void extractAccessTokenEmailAcceptsLegacyTokenWithoutKid() {
        assertThat(claims.extractAccessTokenEmail(buildToken(accessKey, null, null, false)))
                .isEqualTo("user@example.com");
    }

    @Test
    @DisplayName(
            "extractAccessTokenEmail accepts legacy tokens without a kid using the previous access key during rotation")
    void extractAccessTokenEmailAcceptsLegacyPreviousAccessKeyWithoutKid() {
        assertThat(claims.extractAccessTokenEmail(buildToken(previousAccessKey, null, null, false)))
                .isEqualTo("user@example.com");
    }

    @Test
    @DisplayName(
            "extractAccessTokenEmail rejects tokens that advertise the current kid but are signed with the previous key")
    void extractAccessTokenEmailRejectsMismatchedAdvertisedKid() {
        assertThatThrownBy(() ->
                        claims.extractAccessTokenEmail(buildToken(previousAccessKey, "access-active", null, false)))
                .isInstanceOf(JwtTokenException.class);
    }

    private String buildToken(SecretKey key, String keyId, String sessionId, boolean includeVersion) {
        String purpose = includeVersion ? JwtClaimNames.REFRESH_TOKEN_PURPOSE : JwtClaimNames.ACCESS_TOKEN_PURPOSE;
        return buildToken(key, keyId, sessionId, includeVersion, ISSUER, AUDIENCE, purpose);
    }

    private String buildToken(SecretKey key, String sessionId, boolean includeVersion) {
        String purpose = includeVersion ? JwtClaimNames.REFRESH_TOKEN_PURPOSE : JwtClaimNames.ACCESS_TOKEN_PURPOSE;
        return buildToken(key, null, sessionId, includeVersion, ISSUER, AUDIENCE, purpose);
    }

    private String buildToken(SecretKey key, String sessionId, boolean includeVersion, String issuer, String audience) {
        String purpose = includeVersion ? JwtClaimNames.REFRESH_TOKEN_PURPOSE : JwtClaimNames.ACCESS_TOKEN_PURPOSE;
        return buildToken(key, null, sessionId, includeVersion, issuer, audience, purpose);
    }

    private String buildToken(
            SecretKey key,
            String keyId,
            String sessionId,
            boolean includeVersion,
            String issuer,
            String audience,
            String purpose) {
        var builder = Jwts.builder()
                .header()
                .keyId(keyId)
                .and()
                .subject("user@example.com")
                .issuer(issuer)
                .claim(JwtClaimNames.TOKEN_PURPOSE, purpose)
                .audience()
                .add(audience)
                .and()
                .expiration(new Date(System.currentTimeMillis() + 60_000));
        if (sessionId != null) {
            builder.claim("sid", sessionId);
        }
        if (includeVersion) {
            builder.claim("ver", 2);
        }
        return builder.signWith(key).compact();
    }

    private String buildExpiredToken(SecretKey key, String keyId) {
        return Jwts.builder()
                .header()
                .keyId(keyId)
                .and()
                .subject("user@example.com")
                .issuer(ISSUER)
                .claim(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.ACCESS_TOKEN_PURPOSE)
                .audience()
                .add(AUDIENCE)
                .and()
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();
    }

    private static byte[] createBytes(int firstByte) {
        byte[] bytes = new byte[64];
        bytes[0] = (byte) firstByte;
        return bytes;
    }
}

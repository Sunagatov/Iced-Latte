package com.zufar.icedlatte.security.jwt.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.zufar.icedlatte.security.jwt.config.JwtClaimNames;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.jwt.config.JwtSigningKeys;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@DisplayName("JwtTokenProvider unit tests")
class JwtTokenProviderTest {

    // 64-byte base64 key (512 bits) — required for HS512
    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);
    private static final String REFRESH_SECRET = refreshSecret();

    private JwtTokenProvider tokenProvider;
    private SecretKey signingKey;
    private SecretKey refreshSigningKey;

    @BeforeEach
    void setUp() {
        JwtProperties props = properties();
        JwtSigningKeys keyProvider = new JwtSigningKeys(props);
        tokenProvider = new JwtTokenProvider(keyProvider, props);
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        refreshSigningKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(REFRESH_SECRET));
    }

    private UserDetails user(String email) {
        return new User(email, "password", Collections.emptyList());
    }

    @Test
    @DisplayName("Generated access token contains correct subject")
    void generateTokenContainsCorrectSubject() {
        String token = tokenProvider.generateToken(user("alice@example.com"), UUID.randomUUID());

        var jws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
        Claims claims = jws.getPayload();
        assertThat(claims.getSubject()).isEqualTo("alice@example.com");
        assertThat(claims.get(JwtClaimNames.TOKEN_PURPOSE)).isEqualTo(JwtClaimNames.ACCESS_TOKEN_PURPOSE);
        assertThat(jws.getHeader().getKeyId()).isEqualTo("access-active");
    }

    @Test
    @DisplayName("Generated access token is non-blank")
    void generateTokenReturnsNonBlankToken() {
        String token = tokenProvider.generateToken(user("bob@example.com"), UUID.randomUUID());
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Generated refresh token is different from access token")
    void generateRefreshTokenIsDifferentFromAccessToken() {
        UserDetails userDetails = user("carol@example.com");
        UUID sessionId = UUID.randomUUID();
        String accessToken = tokenProvider.generateToken(userDetails, sessionId);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails, sessionId);

        assertThat(refreshToken).isNotEqualTo(accessToken);
    }

    @Test
    @DisplayName("Generated refresh token contains refresh purpose")
    void generateRefreshTokenContainsRefreshPurpose() {
        String token = tokenProvider.generateRefreshToken(user("carol@example.com"), UUID.randomUUID());

        var jws = Jwts.parser().verifyWith(refreshSigningKey).build().parseSignedClaims(token);
        Claims claims = jws.getPayload();

        assertThat(claims.get(JwtClaimNames.TOKEN_PURPOSE)).isEqualTo(JwtClaimNames.REFRESH_TOKEN_PURPOSE);
        assertThat(jws.getHeader().getKeyId()).isEqualTo("refresh-active");
    }

    @Test
    @DisplayName("Access token with extra claims includes those claims")
    void generateTokenWithExtraClaimsIncludesThem() {
        String token = tokenProvider.generateToken(java.util.Map.of("role", "ADMIN"), user("dave@example.com"), null);

        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertThat(claims.get("role")).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Generated support chat websocket ticket contains support chat purpose")
    void generateSupportChatWebSocketTicketContainsSupportChatPurpose() {
        String token = tokenProvider.issue("eve@example.com");

        var jws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
        Claims claims = jws.getPayload();

        assertThat(claims.getSubject()).isEqualTo("eve@example.com");
        assertThat(claims.get(JwtClaimNames.TOKEN_PURPOSE))
                .isEqualTo(JwtClaimNames.SUPPORT_CHAT_WEBSOCKET_TICKET_PURPOSE);
        assertThat(jws.getHeader().getKeyId()).isEqualTo("access-active");
    }

    private static String refreshSecret() {
        byte[] bytes = new byte[64];
        bytes[0] = 1;
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static JwtProperties properties() {
        return new JwtProperties(
                "Authorization",
                SECRET,
                REFRESH_SECRET,
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                "iced-latte",
                "iced-latte-client",
                "access-active",
                "refresh-active",
                null,
                null,
                null,
                null);
    }
}

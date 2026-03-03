package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JwtTokenProvider unit tests")
class JwtTokenProviderTest {

    // 64-byte base64 key (512 bits) — required for HS512
    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);
    private static final String REFRESH_SECRET = Base64.getEncoder().encodeToString(new byte[64]);

    private JwtTokenProvider tokenProvider;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        JwtProperties props = mock(JwtProperties.class);
        when(props.secret()).thenReturn(SECRET);
        when(props.refreshSecret()).thenReturn(REFRESH_SECRET);
        when(props.expiration()).thenReturn(Duration.ofMinutes(15));
        when(props.refreshExpiration()).thenReturn(Duration.ofDays(7));
        when(props.issuer()).thenReturn("iced-latte");
        when(props.audience()).thenReturn("iced-latte-client");

        JwtSignKeyProvider keyProvider = new JwtSignKeyProvider(props);
        tokenProvider = new JwtTokenProvider(keyProvider, props);
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    private UserDetails user(String email) {
        return new User(email, "password", Collections.emptyList());
    }

    @Test
    @DisplayName("Generated access token contains correct subject")
    void generateToken_containsCorrectSubject() {
        String token = tokenProvider.generateToken(user("alice@example.com"));

        Claims claims = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
        assertThat(claims.getSubject()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("Generated access token is non-blank")
    void generateToken_returnsNonBlankToken() {
        String token = tokenProvider.generateToken(user("bob@example.com"));
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Generated refresh token is different from access token")
    void generateRefreshToken_isDifferentFromAccessToken() {
        UserDetails userDetails = user("carol@example.com");
        String accessToken = tokenProvider.generateToken(userDetails);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails);

        assertThat(refreshToken).isNotEqualTo(accessToken);
    }

    @Test
    @DisplayName("Access token with extra claims includes those claims")
    void generateToken_withExtraClaims_includesThem() {
        String token = tokenProvider.generateToken(
                java.util.Map.of("role", "ADMIN"),
                user("dave@example.com")
        );

        Claims claims = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
        assertThat(claims.get("role")).isEqualTo("ADMIN");
    }
}

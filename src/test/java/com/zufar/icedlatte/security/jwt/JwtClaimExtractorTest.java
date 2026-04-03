package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
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

@DisplayName("JwtClaimExtractor unit tests")
class JwtClaimExtractorTest {

    private SecretKey key;
    private JwtClaimExtractor extractor;

    @BeforeEach
    void setUp() {
        key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(new byte[64]);
        JwtSignKeyProvider keyProvider = mock(JwtSignKeyProvider.class);
        when(keyProvider.get()).thenReturn(key);
        extractor = new JwtClaimExtractor(keyProvider);
    }

    private String buildToken(String sid) {
        var builder = Jwts.builder()
                .subject("user@example.com")
                .expiration(new Date(System.currentTimeMillis() + 60_000));
        if (sid != null) builder.claim("sid", sid);
        return builder.signWith(key).compact();
    }

    @Test
    @DisplayName("extractEmail returns subject from valid token")
    void extractEmailReturnsSubject() {
        String token = buildToken(null);
        assertThat(extractor.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("extractEmail throws JwtTokenHasNoUserEmailException for invalid token")
    void extractEmailThrowsForInvalidToken() {
        assertThatThrownBy(() -> extractor.extractEmail("not.a.token"))
                .isInstanceOf(JwtTokenHasNoUserEmailException.class);
    }

    @Test
    @DisplayName("extractSessionId returns UUID when sid claim present")
    void extractSessionIdReturnsSid() {
        UUID sid = UUID.randomUUID();
        String token = buildToken(sid.toString());
        assertThat(extractor.extractSessionId(token)).isEqualTo(Optional.of(sid));
    }

    @Test
    @DisplayName("extractSessionId returns empty when sid claim absent")
    void extractSessionIdReturnsEmptyWhenAbsent() {
        String token = buildToken(null);
        assertThat(extractor.extractSessionId(token)).isEmpty();
    }

    @Test
    @DisplayName("extractSessionId returns empty for invalid token")
    void extractSessionIdReturnsEmptyForInvalidToken() {
        assertThat(extractor.extractSessionId("bad.token")).isEmpty();
    }
}

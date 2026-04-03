package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtRefreshTokenValidator unit tests")
class JwtRefreshTokenValidatorTest {

    @Mock private JwtTokenFromAuthHeaderExtractor tokenExtractor;
    @Mock private JwtBlacklistService blacklistService;

    private JwtRefreshTokenValidator validator;

    @BeforeEach
    void setUp() {
        JwtSignKeyProvider keyProvider = mock(JwtSignKeyProvider.class);
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(new byte[64]);
        when(keyProvider.getRefresh()).thenReturn(key);
        validator = new JwtRefreshTokenValidator(keyProvider, tokenExtractor, blacklistService);
    }

    @Test
    @DisplayName("extractEmail throws JwtTokenBlacklistedException when refresh token is blacklisted")
    void extractEmailBlacklistedTokenThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = "some.refresh.token";
        when(tokenExtractor.extract(request)).thenReturn(token);
        when(blacklistService.isBlacklisted(token)).thenReturn(true);

        assertThatThrownBy(() -> validator.extractEmail(request))
                .isInstanceOf(JwtTokenBlacklistedException.class);
    }

    @Test
    @DisplayName("extractEmail throws JwtTokenHasNoUserEmailException when token is not blacklisted but invalid")
    void extractEmailInvalidTokenThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = "invalid.token.value";
        when(tokenExtractor.extract(request)).thenReturn(token);
        when(blacklistService.isBlacklisted(token)).thenReturn(false);

        assertThatThrownBy(() -> validator.extractEmail(request))
                .isInstanceOf(JwtTokenHasNoUserEmailException.class);
    }

    private SecretKey refreshKey() {
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(new byte[64]);
    }

    private String buildRefreshToken(SecretKey key, String sid, boolean includeVer) {
        var builder = Jwts.builder()
                .subject("user@example.com")
                .expiration(new Date(System.currentTimeMillis() + 60_000));
        if (sid != null) builder.claim("sid", sid);
        if (includeVer) builder.claim("ver", 2);
        return builder.signWith(key).compact();
    }

    @Test
    @DisplayName("isSessionManaged returns true when ver claim present")
    void isSessionManagedReturnsTrueWhenVerPresent() {
        SecretKey key = refreshKey();
        JwtSignKeyProvider keyProvider = mock(JwtSignKeyProvider.class);
        when(keyProvider.getRefresh()).thenReturn(key);
        JwtRefreshTokenValidator v = new JwtRefreshTokenValidator(keyProvider, tokenExtractor, blacklistService);
        String token = buildRefreshToken(key, null, true);
        assertThat(v.isSessionManaged(token)).isTrue();
    }

    @Test
    @DisplayName("isSessionManaged returns false when ver claim absent")
    void isSessionManagedReturnsFalseWhenVerAbsent() {
        SecretKey key = refreshKey();
        JwtSignKeyProvider keyProvider = mock(JwtSignKeyProvider.class);
        when(keyProvider.getRefresh()).thenReturn(key);
        JwtRefreshTokenValidator v = new JwtRefreshTokenValidator(keyProvider, tokenExtractor, blacklistService);
        String token = buildRefreshToken(key, null, false);
        assertThat(v.isSessionManaged(token)).isFalse();
    }

    @Test
    @DisplayName("isSessionManaged returns false for invalid token")
    void isSessionManagedReturnsFalseForInvalidToken() {
        assertThat(validator.isSessionManaged("bad.token")).isFalse();
    }

    @Test
    @DisplayName("extractSessionId returns UUID when sid claim present")
    void extractSessionIdReturnsSid() {
        SecretKey key = refreshKey();
        JwtSignKeyProvider keyProvider = mock(JwtSignKeyProvider.class);
        when(keyProvider.getRefresh()).thenReturn(key);
        JwtRefreshTokenValidator v = new JwtRefreshTokenValidator(keyProvider, tokenExtractor, blacklistService);
        UUID sid = UUID.randomUUID();
        String token = buildRefreshToken(key, sid.toString(), true);
        assertThat(v.extractSessionId(token)).isEqualTo(Optional.of(sid));
    }

    @Test
    @DisplayName("extractSessionId returns empty when sid claim absent")
    void extractSessionIdReturnsEmptyWhenAbsent() {
        SecretKey key = refreshKey();
        JwtSignKeyProvider keyProvider = mock(JwtSignKeyProvider.class);
        when(keyProvider.getRefresh()).thenReturn(key);
        JwtRefreshTokenValidator v = new JwtRefreshTokenValidator(keyProvider, tokenExtractor, blacklistService);
        String token = buildRefreshToken(key, null, false);
        assertThat(v.extractSessionId(token)).isEmpty();
    }

    @Test
    @DisplayName("extractSessionId returns empty for invalid token")
    void extractSessionIdReturnsEmptyForInvalidToken() {
        assertThat(validator.extractSessionId("bad.token")).isEmpty();
    }
}

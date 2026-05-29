package com.zufar.icedlatte.security.jwt.blacklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenBlacklist unit tests")
class JwtTokenBlacklistTest {

    @Mock
    private ExpiringKeyValueStore temporaryStore;

    @Mock
    private JwtProperties jwtProperties;

    private JwtTokenBlacklist service;

    private static final String TOKEN = "test.jwt.token";
    private static final Duration TTL = Duration.ofHours(1);
    private static final Duration REFRESH_TTL = Duration.ofHours(24);

    @BeforeEach
    void setUp() {
        service = new JwtTokenBlacklist(temporaryStore, jwtProperties);
    }

    @Test
    @DisplayName("blacklistToken stores the hashed token with JWT TTL")
    void blacklistTokenStoresHashedTokenWithJwtTtl() {
        when(jwtProperties.expiration()).thenReturn(TTL);

        service.blacklist(TOKEN);

        verify(temporaryStore).put(eq("jwt:blacklist:" + service.hash(TOKEN)), eq("true"), eq(TTL));
    }

    @Test
    @DisplayName("blacklistRefreshToken stores the hashed token with refresh JWT TTL")
    void blacklistRefreshTokenStoresHashedTokenWithRefreshJwtTtl() {
        when(jwtProperties.refreshExpiration()).thenReturn(REFRESH_TTL);

        service.blacklistRefreshToken(TOKEN);

        verify(temporaryStore).put(eq("jwt:blacklist:" + service.hash(TOKEN)), eq("true"), eq(REFRESH_TTL));
    }

    @Test
    @DisplayName("blacklistToken ignores blank tokens")
    void blacklistTokenIgnoresBlankTokens() {
        service.blacklist("   ");

        verifyNoInteractions(temporaryStore, jwtProperties);
    }

    @Test
    @DisplayName("isBlacklisted returns true for blank tokens")
    void isBlacklistedReturnsTrueForBlankTokens() {
        assertThat(service.isBlacklisted("")).isTrue();
        verifyNoInteractions(temporaryStore);
    }

    @Test
    @DisplayName("isBlacklisted delegates to the temporary store")
    void isBlacklistedDelegatesToTemporaryStore() {
        when(temporaryStore.contains("jwt:blacklist:" + service.hash(TOKEN))).thenReturn(true);

        assertThat(service.isBlacklisted(TOKEN)).isTrue();
    }

    @Test
    @DisplayName("isBlacklisted fails closed when the store errors")
    void isBlacklistedFailsClosedWhenStoreErrors() {
        doThrow(new RuntimeException("store down")).when(temporaryStore).contains(any());

        assertThat(service.isBlacklisted(TOKEN)).isTrue();
    }

    @Test
    @DisplayName("validateNotBlacklisted throws when token is blacklisted")
    void validateNotBlacklistedThrowsWhenTokenIsBlacklisted() {
        when(temporaryStore.contains("jwt:blacklist:" + service.hash(TOKEN))).thenReturn(true);

        assertThatThrownBy(() -> service.validateNotBlacklisted(TOKEN))
                .isInstanceOf(JwtTokenBlacklistedException.class);
    }

    @Test
    @DisplayName("validateNotBlacklisted throws when token is blank")
    void validateNotBlacklistedThrowsWhenTokenIsBlank() {
        assertThatThrownBy(() -> service.validateNotBlacklisted(" ")).isInstanceOf(JwtTokenBlacklistedException.class);
    }
}

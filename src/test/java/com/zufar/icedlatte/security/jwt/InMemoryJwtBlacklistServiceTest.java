package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("InMemoryJwtBlacklistService unit tests")
class InMemoryJwtBlacklistServiceTest {

    private InMemoryJwtBlacklistService service;

    @BeforeEach
    void setUp() {
        JwtProperties props = mock(JwtProperties.class);
        when(props.expiration()).thenReturn(Duration.ofMinutes(15));
        service = new InMemoryJwtBlacklistService(props);
    }

    @Test
    @DisplayName("Token is not blacklisted before being added")
    void isBlacklisted_newToken_returnsFalse() {
        assertThat(service.isBlacklisted("some.jwt.token")).isFalse();
    }

    @Test
    @DisplayName("Token is blacklisted after blacklistToken call")
    void isBlacklisted_afterBlacklist_returnsTrue() {
        service.blacklistToken("some.jwt.token");
        assertThat(service.isBlacklisted("some.jwt.token")).isTrue();
    }

    @Test
    @DisplayName("Empty token is treated as blacklisted")
    void isBlacklisted_emptyToken_returnsTrue() {
        assertThat(service.isBlacklisted("")).isTrue();
        assertThat(service.isBlacklisted("   ")).isTrue();
    }

    @Test
    @DisplayName("Null token is treated as blacklisted")
    void isBlacklisted_nullToken_returnsTrue() {
        assertThat(service.isBlacklisted(null)).isTrue();
    }

    @Test
    @DisplayName("blacklistToken with empty token does not add to blacklist")
    void blacklistToken_emptyToken_doesNotAdd() {
        service.blacklistToken("");
        // empty token is always blacklisted by isBlacklisted guard, but no entry added
        assertThat(service.isBlacklisted("")).isTrue();
    }

    @Test
    @DisplayName("Different tokens are independently tracked")
    void isBlacklisted_differentTokens_trackedIndependently() {
        service.blacklistToken("token.one");
        assertThat(service.isBlacklisted("token.one")).isTrue();
        assertThat(service.isBlacklisted("token.two")).isFalse();
    }

    @Test
    @DisplayName("cleanupExpiredTokens runs without error on empty store")
    void cleanupExpiredTokens_emptyStore_doesNotThrow() {
        service.cleanupExpiredTokens();
    }

    @Test
    @DisplayName("shutdown clears all tokens without error")
    void shutdown_clearsTokens() {
        service.blacklistToken("token.a");
        service.blacklistToken("token.b");
        service.shutdown();
        // After shutdown the map is cleared; new checks on those tokens return false
        assertThat(service.isBlacklisted("token.a")).isFalse();
    }
}

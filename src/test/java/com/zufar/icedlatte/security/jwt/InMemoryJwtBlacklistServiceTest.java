package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Nested
    @DisplayName("blacklist and lookup")
    class BlacklistAndLookup {

        @Test
        @DisplayName("token is not blacklisted before being added")
        void isBlacklisted_newToken_returnsFalse() {
            assertThat(service.isBlacklisted("some.jwt.token")).isFalse();
        }

        @Test
        @DisplayName("token is blacklisted after blacklistToken call")
        void isBlacklisted_afterBlacklist_returnsTrue() {
            service.blacklistToken("some.jwt.token");
            assertThat(service.isBlacklisted("some.jwt.token")).isTrue();
        }

        @Test
        @DisplayName("empty and null tokens are treated as blacklisted")
        void emptyAndNullTokensAreTreatedAsBlacklisted() {
            assertThat(service.isBlacklisted("")).isTrue();
            assertThat(service.isBlacklisted("   ")).isTrue();
            assertThat(service.isBlacklisted(null)).isTrue();
        }

        @Test
        @DisplayName("blacklistToken ignores blank values without affecting other tokens")
        void blacklistToken_ignoresBlankValuesWithoutAffectingOtherTokens() {
            service.blacklistToken("valid.token.value");
            service.blacklistToken("");
            service.blacklistToken("   ");

            assertThat(service.isBlacklisted("valid.token.value")).isTrue();
            assertThat(service.isBlacklisted("other.token")).isFalse();
        }

        @Test
        @DisplayName("different tokens are independently tracked")
        void isBlacklisted_differentTokens_trackedIndependently() {
            service.blacklistToken("token.one");
            assertThat(service.isBlacklisted("token.one")).isTrue();
            assertThat(service.isBlacklisted("token.two")).isFalse();
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

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
            assertThat(service.isBlacklisted("token.a")).isFalse();
            assertThat(service.isBlacklisted("token.b")).isFalse();
        }
    }
}

package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.common.temporarycache.InMemoryExpiringKeyValueStore;
import com.zufar.icedlatte.email.exception.TimeTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EmailTokenCooldownCache contract tests")
class TokenTimeExpirationCacheTest {

    private EmailTokenCooldownCache cache;

    @BeforeEach
    void setUp() {
        EmailTokenCooldownCache cooldownCache = new EmailTokenCooldownCache(new InMemoryExpiringKeyValueStore());
        ReflectionTestUtils.setField(cooldownCache, "expireTimeMinutes", 5);
        cache = cooldownCache;
    }

    @Nested
    @DisplayName("validateTimeToken")
    class ValidateTimeToken {

        @Test
        @DisplayName("passes when email has no active cooldown")
        void passesWhenEmailHasNoActiveCooldown() {
            assertThatCode(() -> cache.validateTimeToken("new@example.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws when email was recently used")
        void throwsWhenEmailWasRecentlyUsed() {
            String email = "user@example.com";
            cache.manageEmailSendingRate(email);

            assertThatThrownBy(() -> cache.validateTimeToken(email))
                    .isInstanceOf(TimeTokenException.class);
        }
    }

    @Nested
    @DisplayName("removeToken")
    class RemoveToken {

        @Test
        @DisplayName("clears cooldown so validation passes again")
        void clearsCooldownSoValidationPassesAgain() {
            String email = "user2@example.com";
            cache.manageEmailSendingRate(email);

            cache.removeToken(email);

            assertThatCode(() -> cache.validateTimeToken(email))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("is a no-op for email without cooldown")
        void isANoOpForEmailWithoutCooldown() {
            assertThatCode(() -> cache.removeToken("missing@example.com"))
                    .doesNotThrowAnyException();
        }
    }
}

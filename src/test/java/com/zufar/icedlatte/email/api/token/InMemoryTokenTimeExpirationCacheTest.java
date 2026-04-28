package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.email.exception.TimeTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryTokenTimeExpirationCache")
class InMemoryTokenTimeExpirationCacheTest {

    private final InMemoryTokenTimeExpirationCache cache = new InMemoryTokenTimeExpirationCache(15);

    @Test
    @DisplayName("allows email when no cooldown exists")
    void validateTimeToken_allowsEmailWithoutCooldown() {
        assertThatCode(() -> cache.validateTimeToken("alice@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects email while cooldown is active")
    void validateTimeToken_rejectsActiveCooldown() {
        cache.manageEmailSendingRate("alice@example.com");

        assertThatThrownBy(() -> cache.validateTimeToken("alice@example.com"))
                .isInstanceOf(TimeTokenException.class)
                .hasMessageContaining("alice@example.com");
    }

    @Test
    @DisplayName("allows email again after token removal")
    void removeToken_clearsCooldown() {
        cache.manageEmailSendingRate("alice@example.com");

        cache.removeToken("alice@example.com");

        assertThatCode(() -> cache.validateTimeToken("alice@example.com"))
                .doesNotThrowAnyException();
    }
}

package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.common.temporarycache.ExpiringKeyValueStore;
import com.zufar.icedlatte.email.exception.TimeTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailTokenCooldownCache unit tests")
class EmailTokenCooldownCacheTest {

    @Mock private ExpiringKeyValueStore temporaryStore;

    private EmailTokenCooldownCache cache;

    @BeforeEach
    void setUp() {
        cache = new EmailTokenCooldownCache(temporaryStore);
        ReflectionTestUtils.setField(cache, "expireTimeMinutes", 15);
    }

    @Test
    @DisplayName("manageEmailSendingRate stores the expiry timestamp with TTL")
    void manageEmailSendingRateStoresExpiryTimestampWithTtl() {
        cache.manageEmailSendingRate("alice@example.com");

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(temporaryStore).put(eq("email:rate:alice@example.com"), valueCaptor.capture(), eq(Duration.ofMinutes(15)));
        assertThat(valueCaptor.getValue()).contains("T");
    }

    @Test
    @DisplayName("validateTimeToken is a no-op when no cooldown exists")
    void validateTimeTokenIsNoOpWhenNoCooldownExists() {
        when(temporaryStore.get("email:rate:alice@example.com", String.class)).thenReturn(Optional.empty());

        assertThatCode(() -> cache.validateTimeToken("alice@example.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateTimeToken throws when cooldown exists")
    void validateTimeTokenThrowsWhenCooldownExists() {
        when(temporaryStore.get("email:rate:alice@example.com", String.class))
                .thenReturn(Optional.of("2030-01-01T00:00:00Z"));

        assertThatThrownBy(() -> cache.validateTimeToken("alice@example.com"))
                .isInstanceOf(TimeTokenException.class);
    }

    @Test
    @DisplayName("removeToken deletes the namespaced key")
    void removeTokenDeletesNamespacedKey() {
        cache.removeToken("alice@example.com");

        verify(temporaryStore).remove("email:rate:alice@example.com");
    }
}

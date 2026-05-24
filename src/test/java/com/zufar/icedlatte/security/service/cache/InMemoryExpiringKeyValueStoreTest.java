package com.zufar.icedlatte.security.service.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.common.config.CaffeineSizeProperties;

@DisplayName("InMemoryExpiringKeyValueStore unit tests")
class InMemoryExpiringKeyValueStoreTest {

    private final InMemoryExpiringKeyValueStore store =
            new InMemoryExpiringKeyValueStore(new CaffeineSizeProperties(1_000, 5_000, 10_000, 1_000, 10_000));

    @Test
    @DisplayName("put and get return the stored value")
    void putAndGetReturnStoredValue() {
        store.put("key", "value", Duration.ofMinutes(5));

        assertThat(store.get("key")).contains("value");
    }

    @Test
    @DisplayName("take returns and removes the stored value")
    void takeReturnsAndRemovesStoredValue() {
        store.put("key", "value", Duration.ofMinutes(5));

        assertThat(store.take("key")).contains("value");
        assertThat(store.get("key")).isEmpty();
    }

    @Test
    @DisplayName("remove clears the stored value")
    void removeClearsStoredValue() {
        store.put("key", "value", Duration.ofMinutes(5));

        store.remove("key");

        assertThat(store.get("key")).isEmpty();
    }

    @Test
    @DisplayName("contains reflects whether the key is present")
    void containsReflectsPresence() {
        store.put("key", "true", Duration.ofMinutes(5));

        assertThat(store.contains("key")).isTrue();

        store.remove("key");

        assertThat(store.contains("key")).isFalse();
    }
}

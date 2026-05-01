package com.zufar.icedlatte.common.temporarycache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryExpiringKeyValueStore unit tests")
class InMemoryExpiringKeyValueStoreTest {

    private final InMemoryExpiringKeyValueStore store = new InMemoryExpiringKeyValueStore();

    @Test
    @DisplayName("put and get return the stored value")
    void putAndGetReturnStoredValue() {
        store.put("key", "value", Duration.ofMinutes(5));

        assertThat(store.get("key", String.class)).contains("value");
    }

    @Test
    @DisplayName("take returns and removes the stored value")
    void takeReturnsAndRemovesStoredValue() {
        store.put("key", "value", Duration.ofMinutes(5));

        assertThat(store.take("key", String.class)).contains("value");
        assertThat(store.get("key", String.class)).isEmpty();
    }

    @Test
    @DisplayName("remove clears the stored value")
    void removeClearsStoredValue() {
        store.put("key", "value", Duration.ofMinutes(5));

        store.remove("key");

        assertThat(store.get("key", String.class)).isEmpty();
    }

    @Test
    @DisplayName("contains reflects whether the key is present")
    void containsReflectsPresence() {
        store.put("key", Boolean.TRUE, Duration.ofMinutes(5));

        assertThat(store.contains("key")).isTrue();

        store.remove("key");

        assertThat(store.contains("key")).isFalse();
    }
}

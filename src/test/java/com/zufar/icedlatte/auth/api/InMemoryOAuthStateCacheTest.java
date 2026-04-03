package com.zufar.icedlatte.auth.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryOAuthStateCache unit tests")
class InMemoryOAuthStateCacheTest {

    private final InMemoryOAuthStateCache cache = new InMemoryOAuthStateCache(10);

    @Test
    @DisplayName("store and consume returns stored value")
    void storeAndConsumeReturnsValue() {
        cache.store("nonce1", "https://example.com/callback");
        assertThat(cache.consume("nonce1")).isEqualTo("https://example.com/callback");
    }

    @Test
    @DisplayName("consume removes entry so second consume returns null")
    void consumeRemovesEntry() {
        cache.store("nonce2", "https://example.com/callback");
        cache.consume("nonce2");
        assertThat(cache.consume("nonce2")).isNull();
    }

    @Test
    @DisplayName("consume returns null for unknown nonce")
    void consumeUnknownNonceReturnsNull() {
        assertThat(cache.consume("unknown")).isNull();
    }
}

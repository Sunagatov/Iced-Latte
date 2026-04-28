package com.zufar.icedlatte.auth.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryOAuthStateCache unit tests")
class InMemoryOAuthStateCacheTest {

    private static final String CALLBACK_A = "https://example.com/callback-a";
    private static final String CALLBACK_B = "https://example.com/callback-b";

    private final InMemoryOAuthStateCache cache = new InMemoryOAuthStateCache(10);

    @Nested
    @DisplayName("store and consume")
    class StoreAndConsume {

        @Test
        @DisplayName("returns the stored callback and consumes it once")
        void returnsStoredCallbackAndConsumesItOnce() {
            cache.store("nonce-1", CALLBACK_A);

            assertThat(cache.consume("nonce-1")).isEqualTo(CALLBACK_A);
            assertThat(cache.consume("nonce-1")).isNull();
        }

        @Test
        @DisplayName("replacing an existing nonce keeps only the latest callback")
        void replacingExistingNonceKeepsOnlyLatestCallback() {
            cache.store("nonce-1", CALLBACK_A);
            cache.store("nonce-1", CALLBACK_B);

            assertThat(cache.consume("nonce-1")).isEqualTo(CALLBACK_B);
            assertThat(cache.consume("nonce-1")).isNull();
        }

        @Test
        @DisplayName("does not affect other stored nonces")
        void doesNotAffectOtherStoredNonces() {
            cache.store("nonce-1", CALLBACK_A);
            cache.store("nonce-2", CALLBACK_B);

            assertThat(cache.consume("nonce-2")).isEqualTo(CALLBACK_B);
            assertThat(cache.consume("nonce-1")).isEqualTo(CALLBACK_A);
        }

        @Test
        @DisplayName("returns null for an unknown nonce")
        void returnsNullForUnknownNonce() {
            assertThat(cache.consume("missing")).isNull();
        }
    }
}

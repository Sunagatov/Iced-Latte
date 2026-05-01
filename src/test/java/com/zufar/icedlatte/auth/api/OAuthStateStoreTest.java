package com.zufar.icedlatte.auth.api;

import com.zufar.icedlatte.common.temporarycache.ExpiringKeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthStateStore unit tests")
class OAuthStateStoreTest {

    @Mock private ExpiringKeyValueStore temporaryStore;

    private OAuthStateStore cache;

    @BeforeEach
    void setUp() {
        cache = new OAuthStateStore(temporaryStore);
        ReflectionTestUtils.setField(cache, "ttlMinutes", 10);
    }

    @Test
    @DisplayName("store writes the callback under a namespaced key")
    void storeWritesTheCallbackUnderNamespacedKey() {
        cache.store("nonce-1", "https://example.com/callback");

        verify(temporaryStore).put("oauth:state:nonce-1", "https://example.com/callback", Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("consume returns the stored callback")
    void consumeReturnsStoredCallback() {
        when(temporaryStore.take("oauth:state:nonce-1"))
                .thenReturn(Optional.of("https://example.com/callback"));

        assertThat(cache.consume("nonce-1")).isEqualTo("https://example.com/callback");
    }

    @Test
    @DisplayName("consume returns null when the nonce is absent")
    void consumeReturnsNullWhenNonceIsAbsent() {
        when(temporaryStore.take("oauth:state:missing")).thenReturn(Optional.empty());

        assertThat(cache.consume("missing")).isNull();
    }
}

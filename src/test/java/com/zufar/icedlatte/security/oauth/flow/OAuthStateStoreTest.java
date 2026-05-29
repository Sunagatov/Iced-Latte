package com.zufar.icedlatte.security.oauth.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthStateStore unit tests")
class OAuthStateStoreTest {

    @Mock
    private ExpiringKeyValueStore temporaryStore;

    private OAuthStateStore cache;

    @BeforeEach
    void setUp() {
        cache = new OAuthStateStore(temporaryStore);
        ReflectionTestUtils.setField(cache, "ttlMinutes", 10);
    }

    @Test
    @DisplayName("store writes the callback under a namespaced key")
    void storeWritesTheCallbackUnderNamespacedKey() {
        cache.store(OAuthProvider.GOOGLE, "nonce-1", "https://example.com/callback");

        verify(temporaryStore)
                .put("oauth:state:google:nonce-1", "https://example.com/callback", Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("store rejects non-positive ttl")
    void storeRejectsNonPositiveTtl() {
        ReflectionTestUtils.setField(cache, "ttlMinutes", 0);

        assertThatThrownBy(() -> cache.store(OAuthProvider.GOOGLE, "nonce-1", "https://example.com/callback"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("oauth.state-ttl-minutes must be at least 1 minute, got: 0");
    }

    @Test
    @DisplayName("consume returns the stored callback")
    void consumeReturnsStoredCallback() {
        when(temporaryStore.take("oauth:state:google:nonce-1")).thenReturn(Optional.of("https://example.com/callback"));

        assertThat(cache.consume(OAuthProvider.GOOGLE, "nonce-1")).isEqualTo("https://example.com/callback");
    }

    @Test
    @DisplayName("consume uses a provider-specific key")
    void consumeUsesProviderSpecificKey() {
        when(temporaryStore.take("oauth:state:google:nonce-1")).thenReturn(Optional.empty());

        assertThat(cache.consume(OAuthProvider.GOOGLE, "nonce-1")).isNull();
    }

    @Test
    @DisplayName("consume returns null when the nonce is absent")
    void consumeReturnsNullWhenNonceIsAbsent() {
        when(temporaryStore.take("oauth:state:google:missing")).thenReturn(Optional.empty());

        assertThat(cache.consume(OAuthProvider.GOOGLE, "missing")).isNull();
    }
}

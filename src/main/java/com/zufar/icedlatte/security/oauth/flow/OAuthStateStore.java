package com.zufar.icedlatte.security.oauth.flow;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthStateStore {

    private static final String KEY_PREFIX = "oauth:state:";

    private final ExpiringKeyValueStore temporaryStore;

    @Value("${oauth.state-ttl-minutes:10}")
    private int ttlMinutes;

    public void store(OAuthProvider provider, String nonce, String callbackBase) {
        validateConfiguredTtl();
        temporaryStore.put(namespacedKey(provider, nonce), callbackBase, Duration.ofMinutes(ttlMinutes));
    }

    public String consume(OAuthProvider provider, String nonce) {
        return temporaryStore.take(namespacedKey(provider, nonce)).orElse(null);
    }

    private String namespacedKey(OAuthProvider provider, String nonce) {
        return KEY_PREFIX + provider.id() + ":" + nonce;
    }

    private void validateConfiguredTtl() {
        if (ttlMinutes < 1) {
            throw new IllegalStateException("oauth.state-ttl-minutes must be at least 1 minute, got: " + ttlMinutes);
        }
    }
}

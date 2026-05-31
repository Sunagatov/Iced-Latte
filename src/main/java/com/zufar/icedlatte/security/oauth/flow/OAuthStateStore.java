package com.zufar.icedlatte.security.oauth.flow;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.service.cache.ExpiringKeyValueStore;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuthStateStore {

    private static final String KEY_PREFIX = "oauth:state:";

    private final ExpiringKeyValueStore temporaryStore;
    private final OAuthFlowProperties properties;

    public void store(OAuthProvider provider, String nonce, String callbackBase) {
        temporaryStore.put(namespacedKey(provider, nonce), callbackBase, stateTtl());
    }

    public Optional<String> consume(OAuthProvider provider, String nonce) {
        return temporaryStore.take(namespacedKey(provider, nonce));
    }

    Duration stateTtl() {
        return Duration.ofMinutes(properties.stateTtlMinutes());
    }

    private String namespacedKey(OAuthProvider provider, String nonce) {
        return KEY_PREFIX + provider.id() + ":" + nonce;
    }
}

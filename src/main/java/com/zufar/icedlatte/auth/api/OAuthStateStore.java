package com.zufar.icedlatte.auth.api;

import com.zufar.icedlatte.common.temporarycache.ExpiringKeyValueStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuthStateStore {

    private static final String KEY_PREFIX = "oauth:state:";

    private final ExpiringKeyValueStore temporaryStore;

    @Value("${oauth.state-ttl-minutes:10}")
    private int ttlMinutes;

    public void store(OAuthProvider provider,
                      String nonce,
                      String callbackBase) {
        temporaryStore.put(namespacedKey(provider, nonce), callbackBase, Duration.ofMinutes(ttlMinutes));
    }

    public String consume(OAuthProvider provider,
                          String nonce) {
        return temporaryStore.take(namespacedKey(provider, nonce)).orElse(null);
    }

    private String namespacedKey(OAuthProvider provider,
                                 String nonce) {
        return KEY_PREFIX + provider.id() + ":" + nonce;
    }
}

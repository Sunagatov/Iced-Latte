package com.zufar.icedlatte.auth.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnMissingBean(OAuthStateCache.class)
public class InMemoryOAuthStateCache implements OAuthStateCache {

    private final Cache<String, String> cache;

    public InMemoryOAuthStateCache(@Value("${oauth.state-ttl-minutes:10}") int ttlMinutes) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(1_000)
                .build();
    }

    @Override
    public void store(String nonce,
                      String callbackBase) {
        cache.put(nonce, callbackBase);
    }

    @Override
    public String consume(String nonce) {
        String value = cache.getIfPresent(nonce);
        if (value != null) {
            cache.invalidate(nonce);
        }
        return value;
    }
}

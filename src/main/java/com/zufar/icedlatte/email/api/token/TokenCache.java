package com.zufar.icedlatte.email.api.token;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TokenCache {

    @Value("${spring.temporary-cache-time.token}")
    private static Integer EXPIRE_TIME;
    private final Cache<String, UserRegistrationRequest> tokenCache;

    public TokenCache() {
        this.tokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(EXPIRE_TIME, TimeUnit.MINUTES)
                .build();
    }

    public void addToken(String tokenKey, UserRegistrationRequest request) {
        tokenCache.put(tokenKey, request);
    }

    public UserRegistrationRequest getToken(String tokenKey) {
        return tokenCache.getIfPresent(tokenKey);
    }

    public void removeToken(String tokenKey) {
        tokenCache.invalidate(tokenKey);
    }
}

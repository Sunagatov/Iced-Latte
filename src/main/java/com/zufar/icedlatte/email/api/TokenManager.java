package com.zufar.icedlatte.email.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TokenManager {

    private final Integer EXPIRE_TIME = 5;
    private final Cache<String, UserRegistrationRequest> tokenCache;

    public TokenManager() {
        this.tokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(EXPIRE_TIME, TimeUnit.MINUTES)
                .build();
    }

    public void addToken(String token, UserRegistrationRequest request) {
        tokenCache.put(token, request);
    }

    public UserRegistrationRequest getToken(String token) {
        return tokenCache.getIfPresent(token);
    }

    public void removeToken(String token) {
        tokenCache.invalidate(token);
    }
}

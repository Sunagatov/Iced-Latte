package com.zufar.icedlatte.email.api;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zufar.icedlatte.email.dto.EmailConformationDto;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TokenManager {

    private final Integer EXPIRE_TIME = 5;
    private final Cache<String, EmailConformationDto> tokenCache;

    public TokenManager() {
        this.tokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(EXPIRE_TIME, TimeUnit.MINUTES)
                .build();
    }

    public void addToken(String tokenKey, EmailConformationDto request) {
        tokenCache.put(tokenKey, request);
    }

    public EmailConformationDto getToken(String tokenKey) {
        return tokenCache.getIfPresent(tokenKey);
    }

    public void removeToken(String tokenKey) {
        tokenCache.invalidate(tokenKey);
    }
}

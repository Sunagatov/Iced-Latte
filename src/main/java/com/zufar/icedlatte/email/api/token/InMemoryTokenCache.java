package com.zufar.icedlatte.email.api.token;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zufar.icedlatte.email.exception.IncorrectTokenException;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnMissingBean(TokenCache.class)
public class InMemoryTokenCache implements TokenCache {

    private final Cache<String, UserRegistrationRequest> cache;

    public InMemoryTokenCache(@Value("${temporary-cache.time.token}") Integer expireTime) {
        log.info("token_cache.mode: in-memory (Redis not configured)");
        this.cache = CacheBuilder.newBuilder().expireAfterWrite(expireTime, TimeUnit.MINUTES).build();
    }

    @Override
    public void addToken(String tokenKey, UserRegistrationRequest request) {
        cache.put(tokenKey, request);
    }

    @Override
    public UserRegistrationRequest getToken(String tokenKey) {
        UserRegistrationRequest result = cache.getIfPresent(tokenKey);
        if (result == null) throw new IncorrectTokenException();
        return result;
    }

    @Override
    public void removeToken(String tokenKey) {
        cache.invalidate(tokenKey);
    }
}

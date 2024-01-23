package com.zufar.icedlatte.email.api.token;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zufar.icedlatte.email.exception.TimeTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class TokenTimeExpirationCache {

    private final Integer EXPIRE_TIME;
    private final Cache<String, OffsetDateTime> tokenCache;

    public TokenTimeExpirationCache(@Value("${temporary-cache.time.token}") Integer expireTime) {
        this.EXPIRE_TIME = expireTime;
        this.tokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(EXPIRE_TIME, TimeUnit.MINUTES)
                .build();
    }

    public void manageEmailSendingRate(String email) {
        OffsetDateTime expireTime = OffsetDateTime.now().plus(EXPIRE_TIME, TimeUnit.MINUTES.toChronoUnit());
        tokenCache.put(email, expireTime);
    }

    public void validateTimeToken(String email) {
        OffsetDateTime expireTime = tokenCache.getIfPresent(email);
        if (expireTime != null) {
            throw new TimeTokenException(email, expireTime);
        }
    }

    public void removeToken(String email) {
        tokenCache.invalidate(email);
    }
}


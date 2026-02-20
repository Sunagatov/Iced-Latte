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

    private final int expireTime;
    private final Cache<String, OffsetDateTime> tokenCache;

    public TokenTimeExpirationCache(@Value("${temporary-cache.time.token}") Integer expireTime) {
        this.expireTime = expireTime;
        this.tokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(expireTime, TimeUnit.MINUTES)
                .build();
    }

    public void manageEmailSendingRate(String email) {
        OffsetDateTime expireDateTime = OffsetDateTime.now().plus(expireTime, TimeUnit.MINUTES.toChronoUnit());
        tokenCache.put(email, expireDateTime);
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


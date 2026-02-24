package com.zufar.icedlatte.email.api.token;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zufar.icedlatte.email.exception.TimeTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnMissingBean(TokenTimeExpirationCache.class)
public class InMemoryTokenTimeExpirationCache implements TokenTimeExpirationCache {

    private final int expireTimeMinutes;
    private final Cache<String, OffsetDateTime> cache;

    public InMemoryTokenTimeExpirationCache(@Value("${temporary-cache.time.token}") Integer expireTime) {
        log.info("token_expiration_cache.fallback: Redis unavailable, using in-memory Guava expiration cache");
        this.expireTimeMinutes = expireTime;
        this.cache = CacheBuilder.newBuilder().expireAfterWrite(expireTime, TimeUnit.MINUTES).build();
    }

    @Override
    public void manageEmailSendingRate(String email) {
        cache.put(email, OffsetDateTime.now().plusMinutes(expireTimeMinutes));
    }

    @Override
    public void validateTimeToken(String email) {
        OffsetDateTime expiry = cache.getIfPresent(email);
        if (expiry != null) throw new TimeTokenException(email, expiry);
    }

    @Override
    public void removeToken(String email) {
        cache.invalidate(email);
    }
}

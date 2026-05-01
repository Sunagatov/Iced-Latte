package com.zufar.icedlatte.common.temporarycache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnMissingBean(ExpiringKeyValueStore.class)
public class InMemoryExpiringKeyValueStore implements ExpiringKeyValueStore {

    private static final long MIN_TTL_NANOS = 1L;
    private static final long MAX_ENTRIES = 10_000L;

    private final Cache<String, CacheValue> cache = Caffeine.newBuilder()
            .maximumSize(MAX_ENTRIES)
            .expireAfter(new CacheValueExpiry())
            .build();

    public InMemoryExpiringKeyValueStore() {
        log.info("temporary_cache.mode: in-memory");
    }

    @Override
    public void put(String key, String value, Duration ttl) {
        cache.put(key, new CacheValue(value, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<String> get(String key) {
        CacheValue value = cache.getIfPresent(key);
        return value == null ? Optional.empty() : Optional.of(value.value());
    }

    @Override
    public Optional<String> take(String key) {
        CacheValue value = cache.asMap().remove(key);
        return value == null ? Optional.empty() : Optional.of(value.value());
    }

    @Override
    public void remove(String key) {
        cache.invalidate(key);
    }

    @Override
    public boolean contains(String key) {
        return cache.getIfPresent(key) != null;
    }

    private record CacheValue(String value, Instant expiresAt) { }

    private static final class CacheValueExpiry implements Expiry<String, CacheValue> {

        @Override
        public long expireAfterCreate(@NonNull String key, @NonNull CacheValue value, long currentTime) {
            return ttlNanos(value);
        }

        @Override
        public long expireAfterUpdate(@NonNull String key,
                                      @NonNull CacheValue value,
                                      long currentTime,
                                      long currentDuration) {
            return ttlNanos(value);
        }

        @Override
        public long expireAfterRead(@NonNull String key,
                                    @NonNull CacheValue value,
                                    long currentTime,
                                    long currentDuration) {
            return currentDuration;
        }

        private long ttlNanos(CacheValue value) {
            long nanos = Duration.between(Instant.now(), value.expiresAt()).toNanos();
            return Math.max(MIN_TTL_NANOS, nanos);
        }
    }
}

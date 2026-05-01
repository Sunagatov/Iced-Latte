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
    public void put(String key, Object value, Duration ttl) {
        cache.put(key, new CacheValue(value, Instant.now().plus(ttl)));
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> valueType) {
        CacheValue value = cache.getIfPresent(key);
        return value == null ? Optional.empty() : value.cast(valueType);
    }

    @Override
    public <T> Optional<T> take(String key, Class<T> valueType) {
        CacheValue value = cache.asMap().remove(key);
        return value == null ? Optional.empty() : value.cast(valueType);
    }

    @Override
    public void remove(String key) {
        cache.invalidate(key);
    }

    @Override
    public boolean contains(String key) {
        return cache.getIfPresent(key) != null;
    }

    private record CacheValue(Object value, Instant expiresAt) {

        <T> Optional<T> cast(Class<T> valueType) {
            return valueType.isInstance(value) ? Optional.of(valueType.cast(value)) : Optional.empty();
        }
    }

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

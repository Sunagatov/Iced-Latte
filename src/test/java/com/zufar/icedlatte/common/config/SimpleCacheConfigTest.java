package com.zufar.icedlatte.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

@DisplayName("SimpleCacheConfig unit tests")
class SimpleCacheConfigTest {

    private static final Set<String> EXPECTED_CACHE_NAMES =
            Set.of("productById", "brands", "sellers", "productImageUrl", "productImageUrls");

    private final SimpleCacheConfig config = new SimpleCacheConfig(List.of(new TestCacheConfigurationProvider()));

    @Test
    @DisplayName("cacheManager exposes the expected named caches")
    void cacheManagerExposesExpectedNamedCaches() {
        var cacheManager = config.cacheManager();

        assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrderElementsOf(EXPECTED_CACHE_NAMES);

        EXPECTED_CACHE_NAMES.forEach(
                cacheName -> assertThat(cacheManager.getCache(cacheName)).isInstanceOf(ConcurrentMapCache.class));
    }

    @Test
    @DisplayName("cacheManager creates independent cache instances")
    void cacheManagerCreatesIndependentCacheInstances() {
        var cacheManager = config.cacheManager();
        var productByIdCache = cacheManager.getCache("productById");
        var brandsCache = cacheManager.getCache("brands");

        assertThat(productByIdCache).isNotSameAs(brandsCache);

        Objects.requireNonNull(productByIdCache).put("p1", "value-1");
        Objects.requireNonNull(brandsCache).put("b1", "value-2");

        assertThat(productByIdCache.get("p1", String.class)).isEqualTo("value-1");
        assertThat(productByIdCache.get("b1")).isNull();
        assertThat(brandsCache.get("b1", String.class)).isEqualTo("value-2");
        assertThat(brandsCache.get("p1")).isNull();
    }

    private static final class TestCacheConfigurationProvider implements CacheConfigurationProvider {

        @Override
        public Set<String> cacheNames() {
            return EXPECTED_CACHE_NAMES;
        }

        @Override
        public Map<String, RedisCacheConfiguration> redisCacheConfigurations(
                RedisCacheConfiguration baseConfiguration) {
            return Map.of();
        }
    }
}

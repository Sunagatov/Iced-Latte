package com.zufar.icedlatte.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleCacheConfigTest {

    private final SimpleCacheConfig config = new SimpleCacheConfig();

    @Test
    void cacheManagerContainsExpectedCaches() {
        CacheManager cacheManager = config.cacheManager();

        assertNotNull(cacheManager);
        assertTrue(cacheManager instanceof ConcurrentMapCacheManager);

        for (String name : new String[]{"productById", "brands", "sellers", "productImageUrl", "productImageUrls"}) {
            assertNotNull(cacheManager.getCache(name), "Missing cache: " + name);
        }
    }
}

package com.zufar.icedlatte.product.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductCacheEvictor unit tests")
class ProductCacheEvictorTest {

    private final ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("productById");
    private final ProductCacheEvictor evictor = new ProductCacheEvictor(cacheManager);

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("evicts product cache immediately when no transaction synchronization is active")
    void evictsImmediatelyWithoutTransactionSynchronization() {
        UUID productId = UUID.randomUUID();
        var cache = cacheManager.getCache("productById");
        assertThat(cache).isNotNull();
        cache.put(productId, "cached-product");

        evictor.evictProductByIdAfterCommit(productId);

        assertThat(cache.get(productId)).isNull();
    }

    @Test
    @DisplayName("defers product cache eviction until transaction commit")
    void defersEvictionUntilTransactionCommit() {
        UUID productId = UUID.randomUUID();
        var cache = cacheManager.getCache("productById");
        assertThat(cache).isNotNull();
        cache.put(productId, "cached-product");

        TransactionSynchronizationManager.initSynchronization();
        evictor.evictProductByIdAfterCommit(productId);

        assertThat(cache.get(productId, String.class)).isEqualTo("cached-product");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        assertThat(cache.get(productId)).isNull();
    }

    @Test
    @DisplayName("clears product cache after transaction commit")
    void clearsProductCacheAfterTransactionCommit() {
        var cache = cacheManager.getCache("productById");
        assertThat(cache).isNotNull();
        cache.put("first", "cached-product-1");
        cache.put("second", "cached-product-2");

        TransactionSynchronizationManager.initSynchronization();
        evictor.clearProductByIdAfterCommit();

        assertThat(cache.get("first", String.class)).isEqualTo("cached-product-1");
        assertThat(cache.get("second", String.class)).isEqualTo("cached-product-2");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        assertThat(cache.get("first")).isNull();
        assertThat(cache.get("second")).isNull();
    }
}

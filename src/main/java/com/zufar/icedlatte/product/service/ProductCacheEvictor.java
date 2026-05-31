package com.zufar.icedlatte.product.service;

import java.util.UUID;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.zufar.icedlatte.product.config.ProductCacheConfigurationProvider;

import lombok.RequiredArgsConstructor;

/**
 * Evicts product caches after the surrounding database transaction commits.
 *
 * <p>This class is intentionally more explicit than a plain {@code @CacheEvict}. Product review changes update cached
 * fields on {@code ProductInfo}, such as average rating, review count, and AI summary. If the cache is evicted before
 * the transaction commits, another request can miss the cache, read the old database row, and put that old value back
 * into {@code productById}. Then the database commit succeeds, but the cache still contains stale product data.
 *
 * <p>By registering eviction in {@link TransactionSynchronization#afterCommit()}, the cache is cleared only after the
 * database change is visible to later transactions. When no transaction synchronization is active, eviction runs
 * immediately; this keeps the helper useful in unit tests and non-transactional maintenance code.
 */
@Service
@RequiredArgsConstructor
public class ProductCacheEvictor {

    private final CacheManager cacheManager;

    public void evictProductByIdAfterCommit(UUID productId) {
        runAfterCommit(() -> {
            Cache cache = cacheManager.getCache(ProductCacheConfigurationProvider.PRODUCT_BY_ID);
            if (cache != null) {
                cache.evict(productId);
            }
        });
    }

    public void clearProductByIdAfterCommit() {
        runAfterCommit(() -> {
            Cache cache = cacheManager.getCache(ProductCacheConfigurationProvider.PRODUCT_BY_ID);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    private static void runAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }
}

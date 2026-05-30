package com.zufar.icedlatte.product.service;

import java.util.UUID;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductCacheEvictor {

    private static final String PRODUCT_BY_ID_CACHE = "productById";

    private final CacheManager cacheManager;

    public void evictProductByIdAfterCommit(UUID productId) {
        runAfterCommit(() -> {
            Cache cache = cacheManager.getCache(PRODUCT_BY_ID_CACHE);
            if (cache != null) {
                cache.evict(productId);
            }
        });
    }

    public void clearProductByIdAfterCommit() {
        runAfterCommit(() -> {
            Cache cache = cacheManager.getCache(PRODUCT_BY_ID_CACHE);
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

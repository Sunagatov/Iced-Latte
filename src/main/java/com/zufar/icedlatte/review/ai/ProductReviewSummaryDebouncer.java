package com.zufar.icedlatte.review.ai;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewSummaryDebouncer {

    private static final long DEBOUNCE_DELAY_SEC = 120;
    private static final long MAX_WAIT_SEC = 600;

    private final ProductSummaryService productSummaryService;
    private final ProductInfoRepository productInfoRepository;
    private final ApplicationContext applicationContext;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> pendingDebounce = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> firstTriggerTime = new ConcurrentHashMap<>();

    public void schedule(UUID productId) {
        long now = System.currentTimeMillis();
        firstTriggerTime.putIfAbsent(productId, now);

        long elapsed = now - firstTriggerTime.get(productId);
        long delay = elapsed >= MAX_WAIT_SEC * 1000
                ? 0
                : DEBOUNCE_DELAY_SEC;

        ScheduledFuture<?> existing = pendingDebounce.remove(productId);
        if (existing != null) existing.cancel(false);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                applicationContext.getBean(ProductReviewSummaryDebouncer.class).runSummary(productId);
            } catch (Exception e) {
                log.warn("product.ai_summary.schedule.failed: productId={} cause={}", productId, e.getMessage(), e);
            }
        }, delay, TimeUnit.SECONDS);
        pendingDebounce.put(productId, future);
    }

    @CacheEvict(cacheNames = "productById", key = "#productId")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runSummary(UUID productId) {
        pendingDebounce.remove(productId);
        firstTriggerTime.remove(productId);
        try {
            var summary = productSummaryService.summarize(productId);
            productInfoRepository.findById(productId).ifPresent(product -> {
                product.setAiSummary(summary);
                productInfoRepository.save(product);
                log.info("product.ai_summary.updated: productId={}", productId);
            });
        } catch (Exception e) {
            log.warn("product.ai_summary.failed: productId={} cause={}", productId, e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}

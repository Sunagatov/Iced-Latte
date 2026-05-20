package com.zufar.icedlatte.review.ai.summary;

import com.zufar.icedlatte.product.api.ProductReviewProductGateway;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ProductReviewSummaryDebouncer {

    private final long debounceDelaySec;
    private final long maxWaitSec;
    private final ProductSummaryService productSummaryService;
    private final ProductReviewProductGateway productReviewProductGateway;
    private final ApplicationContext applicationContext;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> pendingDebounce = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> firstTriggerTime = new ConcurrentHashMap<>();

    public ProductReviewSummaryDebouncer(
            @Value("${ai.review-summary.debounce-delay:PT2M}") Duration debounceDelay,
            @Value("${ai.review-summary.max-wait:PT10M}") Duration maxWait,
            ProductSummaryService productSummaryService,
            ProductReviewProductGateway productReviewProductGateway,
            ApplicationContext applicationContext) {
        this.debounceDelaySec = debounceDelay.toSeconds();
        this.maxWaitSec = maxWait.toSeconds();
        this.productSummaryService = productSummaryService;
        this.productReviewProductGateway = productReviewProductGateway;
        this.applicationContext = applicationContext;
    }

    public void schedule(UUID productId) {
        long now = System.currentTimeMillis();
        firstTriggerTime.putIfAbsent(productId, now);

        long elapsed = now - firstTriggerTime.get(productId);
        long delay = elapsed >= maxWaitSec * 1000
                ? 0
                : debounceDelaySec;

        ScheduledFuture<?> existing = pendingDebounce.remove(productId);
        if (existing != null) existing.cancel(false);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                applicationContext.getBean(ProductReviewSummaryDebouncer.class).runSummary(productId);
            } catch (Exception e) {
                log.warn("product.ai_summary.schedule.failed: productId={}, exceptionClass={}",
                        productId, e.getClass().getSimpleName(), e);
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
            productReviewProductGateway.updateAiSummary(productId, summary);
            log.info("product.ai_summary.updated: productId={}", productId);
        } catch (Exception e) {
            log.warn("product.ai_summary.failed: productId={}, exceptionClass={}",
                    productId, e.getClass().getSimpleName());
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}

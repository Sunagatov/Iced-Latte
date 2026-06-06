package com.zufar.icedlatte.review.service.ai.summary;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.zufar.icedlatte.product.api.ProductReviewProductApi;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductReviewSummaryDebouncer {

    private final long debounceDelaySec;
    private final long maxWaitSec;
    private final int maxRetryAttempts;
    private final ProductSummaryService productSummaryService;
    private final ProductReviewProductApi productReviewProductApi;
    private final ObjectProvider<ProductReviewSummaryDebouncer> selfProvider;

    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> pendingDebounce = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> firstTriggerTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> retryCounts = new ConcurrentHashMap<>();

    @Autowired
    public ProductReviewSummaryDebouncer(
            @Value("${ai.review-summary.debounce-delay:PT2M}") Duration debounceDelay,
            @Value("${ai.review-summary.max-wait:PT10M}") Duration maxWait,
            @Value("${ai.review-summary.max-retry-attempts:3}") int maxRetryAttempts,
            ProductSummaryService productSummaryService,
            ProductReviewProductApi productReviewProductApi,
            ObjectProvider<ProductReviewSummaryDebouncer> selfProvider) {
        this(
                debounceDelay,
                maxWait,
                maxRetryAttempts,
                productSummaryService,
                productReviewProductApi,
                selfProvider,
                Executors.newScheduledThreadPool(2));
    }

    ProductReviewSummaryDebouncer(
            Duration debounceDelay,
            Duration maxWait,
            int maxRetryAttempts,
            ProductSummaryService productSummaryService,
            ProductReviewProductApi productReviewProductApi,
            ObjectProvider<ProductReviewSummaryDebouncer> selfProvider,
            ScheduledExecutorService scheduler) {
        this.debounceDelaySec = debounceDelay.toSeconds();
        this.maxWaitSec = maxWait.toSeconds();
        this.maxRetryAttempts = maxRetryAttempts;
        this.productSummaryService = productSummaryService;
        this.productReviewProductApi = productReviewProductApi;
        this.selfProvider = selfProvider;
        this.scheduler = scheduler;
    }

    public void schedule(UUID productId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    scheduleAfterCommit(productId);
                }
            });
            return;
        }
        scheduleAfterCommit(productId);
    }

    private void scheduleAfterCommit(UUID productId) {
        long now = System.currentTimeMillis();
        firstTriggerTime.putIfAbsent(productId, now);

        long elapsed = now - firstTriggerTime.get(productId);
        long delay = elapsed >= maxWaitSec * 1000 ? 0 : debounceDelaySec;

        ScheduledFuture<?> existing = pendingDebounce.remove(productId);
        if (existing != null) existing.cancel(false);

        ScheduledFuture<?> future = scheduler.schedule(
                () -> {
                    try {
                        selfProvider.getObject().runSummary(productId);
                    } catch (Exception e) {
                        log.warn(
                                "product.ai_summary.schedule.failed: productId={}, exceptionClass={}",
                                productId,
                                e.getClass().getSimpleName(),
                                e);
                    }
                },
                delay,
                TimeUnit.SECONDS);
        pendingDebounce.compute(productId, (_, _) -> future.isDone() ? null : future);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runSummary(UUID productId) {
        pendingDebounce.remove(productId);
        firstTriggerTime.remove(productId);
        try {
            var summary = productSummaryService.summarize(productId);
            if (summary == null || summary.isBlank()) {
                log.info("product.ai_summary.skipped: productId={}, reason=EMPTY_SUMMARY", productId);
                return;
            }
            productReviewProductApi.updateAiSummary(productId, summary);
            retryCounts.remove(productId);
            log.info("product.ai_summary.updated: productId={}", productId);
        } catch (Exception e) {
            int retryCount = retryCounts.merge(productId, 1, Integer::sum);
            log.warn(
                    "product.ai_summary.failed: productId={}, retryCount={}, maxRetryAttempts={}, exceptionClass={}",
                    productId,
                    retryCount,
                    maxRetryAttempts,
                    e.getClass().getSimpleName());
            if (retryCount > maxRetryAttempts) {
                retryCounts.remove(productId);
                log.warn("product.ai_summary.retry_exhausted: productId={}", productId);
                return;
            }
            schedule(productId);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}

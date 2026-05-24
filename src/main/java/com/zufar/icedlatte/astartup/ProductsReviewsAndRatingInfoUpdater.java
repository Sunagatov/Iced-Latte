package com.zufar.icedlatte.astartup;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.zufar.icedlatte.product.api.ProductReviewProductApi;
import com.zufar.icedlatte.review.api.ReviewMaintenanceApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductsReviewsAndRatingInfoUpdater implements ApplicationRunner {

    @Value("${migration.ratings.enabled:false}")
    private boolean enabled;

    @Value("${migration.timeout-minutes:5}")
    private int timeoutMinutes;

    private final ProductReviewProductApi productReviewProductApi;
    private final ReviewMaintenanceApi reviewMaintenanceApi;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!enabled) {
            log.info("migration.ratings.skipped: reason=disabled");
            return;
        }
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        Runnable runnableJob = () -> transactionTemplate.executeWithoutResult(_ -> {
                    log.info("migration.ratings.start");
                    long t0 = System.currentTimeMillis();
                    productReviewProductApi.refreshAllReviewAggregates();
                    reviewMaintenanceApi.refreshAllCounts();
                    log.info("migration.ratings.finish: durationMs={}", System.currentTimeMillis() - t0);
                }
        );

        CompletableFuture.runAsync(runnableJob, executor)
                .orTimeout(timeoutMinutes, java.util.concurrent.TimeUnit.MINUTES)
                .whenComplete((_, e) -> {
                    executor.close();
                    if (e != null) {
                        log.error("migration.ratings.error: message={}", e.getMessage(), e);
                    }
                });
    }
}

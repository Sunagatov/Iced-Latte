package com.zufar.icedlatte.astartup;

import java.time.Duration;

import org.jspecify.annotations.NonNull;
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

    private final ProductReviewProductApi productReviewProductApi;
    private final ReviewMaintenanceApi reviewMaintenanceApi;
    private final TransactionTemplate transactionTemplate;
    private final MigrationProperties migrationProperties;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!migrationProperties.ratings().enabled()) {
            log.info("migration.ratings.skipped: reason=disabled");
            return;
        }
        StartupTaskRunner.runAsync(
                "ratings startup migration", migrationProperties.timeout(), this::refreshReviewMetadata);
    }

    private void refreshReviewMetadata() {
        transactionTemplate.executeWithoutResult(_ -> {
            log.info("migration.ratings.start");
            long t0 = System.nanoTime();
            productReviewProductApi.refreshAllReviewAggregates();
            reviewMaintenanceApi.refreshAllCounts();
            long duration = Duration.ofNanos(System.nanoTime() - t0).toMillis();
            log.info("migration.ratings.finish: durationMs={}", duration);
        });
    }
}

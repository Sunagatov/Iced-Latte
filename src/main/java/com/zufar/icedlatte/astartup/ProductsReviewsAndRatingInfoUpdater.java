package com.zufar.icedlatte.astartup;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductsReviewsAndRatingInfoUpdater implements ApplicationRunner {

    private final ProductInfoRepository productInfoRepository;
    private final ProductReviewRepository productReviewRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        CompletableFuture.runAsync(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    productInfoRepository.updateAllAverageRatings();
                    productInfoRepository.updateAllReviewsCounts();
                    productReviewRepository.updateAllLikesCounts();
                    productReviewRepository.updateAllDislikesCounts();
                    log.info("migration.ratings.finish");
                }), executor)
            .orTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
            .whenComplete((v, e) -> {
                executor.close();
                if (e != null) log.error("migration.ratings.error: message={}", e.getMessage(), e);
            });
    }
}

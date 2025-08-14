package com.zufar.icedlatte.astartup;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductsReviewsAndRatingInfoUpdater implements ApplicationRunner {

    private final ProductInfoRepository productInfoRepository;
    private final ProductReviewRepository productReviewRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws SQLException {
        try {
            productInfoRepository.findAll().stream()
                .map(product -> product.getProductId())
                .forEach(productId -> {
                    productInfoRepository.updateAverageRating(productId);
                    productInfoRepository.updateReviewsCount(productId);
                });

            productReviewRepository.findAll().stream()
                .map(review -> review.getId())
                .forEach(reviewId -> {
                    productReviewRepository.updateLikesCount(reviewId);
                    productReviewRepository.updateDislikesCount(reviewId);
                });

            log.info("Product reviews and ratings update completed successfully");
            
        } catch (Exception e) {
            var errorMessage = switch (e) {
                case RuntimeException re -> "Runtime error during product update: " + re.getMessage();
                case Exception ex -> "Unexpected error during product update: " + ex.getMessage();
            };
            log.error(errorMessage, e);
            throw new SQLException(errorMessage, e);
        }
    }
}

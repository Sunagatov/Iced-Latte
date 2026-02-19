package com.zufar.icedlatte.astartup;

import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class ProductsReviewsAndRatingInfoUpdater implements ApplicationRunner {

    private final ProductInfoRepository productInfoRepository;
    private final ProductReviewRepository productReviewRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        productInfoRepository.findAll().stream()
            .map(ProductInfo::getId)
            .forEach(productId -> {
                productInfoRepository.updateAverageRating(productId);
                productInfoRepository.updateReviewsCount(productId);
            });

        productReviewRepository.findAll().stream()
            .map(ProductReview::getId)
            .forEach(reviewId -> {
                productReviewRepository.updateLikesCount(reviewId);
                productReviewRepository.updateDislikesCount(reviewId);
            });

        log.info("Product reviews and ratings update completed successfully");
    }
}

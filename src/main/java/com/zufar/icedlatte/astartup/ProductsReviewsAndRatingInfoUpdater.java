package com.zufar.icedlatte.astartup;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
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
        productInfoRepository.updateAllAverageRatings();
        productInfoRepository.updateAllReviewsCounts();
        productReviewRepository.updateAllLikesCounts();
        productReviewRepository.updateAllDislikesCounts();
        log.info("Product reviews and ratings update completed successfully");
    }
}

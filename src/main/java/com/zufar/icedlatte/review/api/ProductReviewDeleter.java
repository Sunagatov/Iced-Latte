package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewDeleter {

    private final ProductReviewRepository reviewRepository;
    private final ProductReviewValidator productReviewValidator;
    private final ProductInfoRepository productInfoRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID productId,
                       final UUID productReviewId) {
        productReviewValidator.validateProductReviewDeletionAllowed(productReviewId);
        productReviewValidator.validateProductExists(productId);

        reviewRepository.deleteById(productReviewId);

        productInfoRepository.updateAverageRating(productId);
        productInfoRepository.updateReviewsCount(productId);
    }
}

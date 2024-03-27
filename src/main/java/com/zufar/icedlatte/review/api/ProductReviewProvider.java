package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.exception.ProductReviewNotFoundException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
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
public class ProductReviewProvider {

    private final ProductReviewRepository reviewRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReview getReviewEntityById(final UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.warn("Failed to get the review entity with id: {}", reviewId);
                    return new ProductReviewNotFoundException(reviewId);
                });
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Integer getReviewCountProductById(final UUID productId) {
        return reviewRepository.getReviewCountProductById(productId)
                .orElseThrow(() -> {
                    log.warn("Failed to get the review entities for product id: {}", productId);
                    return new ProductReviewNotFoundException(productId);
                });
    }
}

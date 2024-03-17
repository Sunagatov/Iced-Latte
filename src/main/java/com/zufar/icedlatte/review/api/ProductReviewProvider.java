package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductReviewStatus;
import com.zufar.icedlatte.product.api.SingleProductProvider;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.exception.ProductReviewNotFoundException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
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
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final SingleProductProvider singleProductProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReview getReviewEntityById(final UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.warn("Failed to get the review entity with id: {}", reviewId);
                    return new ProductReviewNotFoundException(reviewId);
                });
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReviewStatus getProductReviewByUser(UUID productId) {
        singleProductProvider.getProductEntityById(productId);
        var userId = securityPrincipalProvider.getUserId();
        var review = reviewRepository.findByUserIdAndProductInfoProductId(userId, productId);
        return new ProductReviewStatus().exists(review.isPresent());
    }
}

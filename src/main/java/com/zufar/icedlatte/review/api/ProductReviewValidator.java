package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.product.api.SingleProductProvider;
import com.zufar.icedlatte.review.exception.DeniedProductReviewCreationException;
import com.zufar.icedlatte.review.exception.DeniedProductReviewDeletionException;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewValidator {

    private final SingleProductProvider singleProductProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final ProductReviewProvider productReviewProvider;
    private final ProductReviewRepository reviewRepository;

    public void validateReview(final UUID userId, final UUID productId, final String text) {
        if (text.isEmpty()) {
            throw new EmptyProductReviewException();
        }
        validateReviewExists(userId, productId);
    }

    /**
     * Check if product exists
     */
    public void validateProductExists(final UUID productId) {
        singleProductProvider.getProductEntityById(productId);
    }

    /**
     * Check if user has already created a review for this product
     */
    public void validateReviewExists(final UUID userId, final UUID productId) {
        var review = reviewRepository.findByUserIdAndProductInfoProductId(userId, productId);
        if (review.isPresent()) {
            throw new DeniedProductReviewCreationException(productId, userId, review.get().getId());
        }
    }


    public void validateProductReviewDeletionAllowed(final UUID productReviewId) {
        var currentUserId = securityPrincipalProvider.getUserId();
        var creatorId = productReviewProvider.getReviewEntityById(productReviewId).getUser().getId();

        if (!currentUserId.equals(creatorId)) {
            throw new DeniedProductReviewDeletionException(productReviewId, currentUserId);
        }
    }
}

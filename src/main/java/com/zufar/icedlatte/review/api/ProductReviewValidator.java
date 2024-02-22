package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.product.api.SingleProductProvider;
import com.zufar.icedlatte.review.exception.DeniedProductReviewDeletionException;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
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

    public void validateReview(final UUID productId, final String text) {
        if (text.isEmpty()) {
            throw new EmptyProductReviewException();
        }
        validateProductExists(productId);
    }

    public void validateProductExists(final UUID productId) {
        // check if product exists
        singleProductProvider.getProductEntityById(productId);
    }


    public void validateProductReviewDeletionAllowed(final UUID productReviewId) {
        var currentUserId = securityPrincipalProvider.getUserId();
        var creatorId = productReviewProvider.getReviewEntityById(productReviewId).getUserId();

        if (!currentUserId.equals(creatorId)) {
            throw new DeniedProductReviewDeletionException(productReviewId, currentUserId);
        }
    }
}

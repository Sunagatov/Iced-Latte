package com.zufar.icedlatte.review.validator;

import com.zufar.icedlatte.product.api.ProductReviewProductGateway;
import com.zufar.icedlatte.review.exception.DeniedProductReviewCreationException;
import com.zufar.icedlatte.review.exception.DeniedProductReviewDeletionException;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
import com.zufar.icedlatte.review.exception.ProductNotFoundForReviewException;
import com.zufar.icedlatte.review.exception.ProductReviewNotFoundException;
import com.zufar.icedlatte.review.exception.InvalidProductReviewTextException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewValidator {

    private final ProductReviewRepository productReviewRepository;
    private final ProductReviewProductGateway productReviewProductGateway;

    private static final Pattern INVALID_REVIEW_TEXT_PATTERN = Pattern.compile("[<>{}\\[\\]|\\\\^~`]");

    public void validateReviewText(final String productReviewText) {
        if (productReviewText.trim().isEmpty()) {
            throw new EmptyProductReviewException();
        }
        if (INVALID_REVIEW_TEXT_PATTERN.matcher(productReviewText).find()) {
            throw new InvalidProductReviewTextException();
        }
    }

    public void validateProductExists(final UUID productId) {
        if (!productReviewProductGateway.exists(productId)) {
            throw new ProductNotFoundForReviewException(productId);
        }
    }

    public void validateReviewExistsForUser(final UUID userId, final UUID productId) {
        var productReview = productReviewRepository.findByUserIdAndProductId(userId, productId);
        if (productReview.isPresent()) {
            throw new DeniedProductReviewCreationException(userId, productId, productReview.get().getId());
        }
    }

    public void validateProductReviewDeletionAllowed(final UUID productReviewId, final UUID currentUserId) {
        var review = productReviewRepository.findById(productReviewId)
                .orElseThrow(() -> new ProductReviewNotFoundException(productReviewId));
        if (!currentUserId.equals(review.getUser().getId())) {
            throw new DeniedProductReviewDeletionException(productReviewId, currentUserId);
        }
    }

    public void validateProductIdIsValid(final UUID productId, final UUID productReviewId) {
        if (!productReviewProductGateway.exists(productId)) {
            throw new ProductNotFoundForReviewException(productId);
        }
        if (!productReviewRepository.existsByIdAndProductId(productReviewId, productId)) {
            throw new ProductReviewNotFoundException(productReviewId);
        }
    }
}

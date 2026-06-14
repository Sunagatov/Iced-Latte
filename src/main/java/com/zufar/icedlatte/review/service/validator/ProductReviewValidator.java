package com.zufar.icedlatte.review.service.validator;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.product.api.ProductReviewProductApi;
import com.zufar.icedlatte.review.exception.ReviewAccessDeniedException;
import com.zufar.icedlatte.review.exception.ReviewConflictException;
import com.zufar.icedlatte.review.exception.ReviewNotFoundException;
import com.zufar.icedlatte.review.exception.ReviewProductNotFoundException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewValidator {

    private final ProductReviewRepository productReviewRepository;
    private final ProductReviewProductApi productReviewProductApi;

    private static final Pattern INVALID_REVIEW_TEXT_PATTERN = Pattern.compile("[<>{}\\[\\]|\\\\^~`]");
    private static final int MIN_PRODUCT_RATING = 1;
    private static final int MAX_PRODUCT_RATING = 5;

    public void validateReviewText(final String productReviewText) {
        if (productReviewText == null || productReviewText.trim().isEmpty()) {
            throw new BadRequestException("Product's review is empty");
        }
        if (INVALID_REVIEW_TEXT_PATTERN.matcher(productReviewText).find()) {
            throw new BadRequestException("The Product Review Text Is Invalid.");
        }
    }

    public void validateProductRating(final Integer productRating) {
        if (productRating == null || productRating < MIN_PRODUCT_RATING || productRating > MAX_PRODUCT_RATING) {
            throw new BadRequestException("Product's review rating must be between 1 and 5");
        }
    }

    public void validateProductExists(final UUID productId) {
        if (!productReviewProductApi.exists(productId)) {
            throw new ReviewProductNotFoundException(productId);
        }
    }

    public void validateReviewExistsForUser(final UUID userId, final UUID productId) {
        var productReview = productReviewRepository.findByUserIdAndProductId(userId, productId);
        if (productReview.isPresent()) {
            throw new ReviewConflictException(String.format(
                    "Creation of the product's review for the user with userId = '%s' and the product with productId = '%s' is denied. Delete the previous product's review '%s' first.",
                    userId, productId, productReview.get().getId()));
        }
    }

    public void validateProductReviewDeletionAllowed(final UUID productReviewId, final UUID currentUserId) {
        var review = productReviewRepository
                .findById(productReviewId)
                .orElseThrow(() -> new ReviewNotFoundException(productReviewId));
        if (!currentUserId.equals(review.getUserId())) {
            throw new ReviewAccessDeniedException();
        }
    }

    public void validateProductIdIsValid(final UUID productId, final UUID productReviewId) {
        if (!productReviewProductApi.exists(productId)) {
            throw new ReviewProductNotFoundException(productId);
        }
        if (!productReviewRepository.existsByIdAndProductId(productReviewId, productId)) {
            throw new ReviewNotFoundException(productReviewId);
        }
    }
}

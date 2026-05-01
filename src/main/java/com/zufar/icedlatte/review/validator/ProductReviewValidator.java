package com.zufar.icedlatte.review.validator;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.exception.NotFoundException;
import com.zufar.icedlatte.product.api.ProductReviewProductGateway;
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
            throw new BadRequestException("Product's review is empty");
        }
        if (INVALID_REVIEW_TEXT_PATTERN.matcher(productReviewText).find()) {
            throw new BadRequestException("The Product Review Text Is Invalid.");
        }
    }

    public void validateProductExists(final UUID productId) {
        if (!productReviewProductGateway.exists(productId)) {
            throw new NotFoundException(String.format(
                    "Product with productId = '%s' was not found. Product's review operations (update, delete, provide) are not possible.",
                    productId));
        }
    }

    public void validateReviewExistsForUser(final UUID userId, final UUID productId) {
        var productReview = productReviewRepository.findByUserIdAndProductId(userId, productId);
        if (productReview.isPresent()) {
            throw new BadRequestException(String.format(
                    "Creation of the product's review for the user with userId = '%s' and the product with productId = '%s' is denied. Delete the previous product's review '%s' first.",
                    userId, productId, productReview.get().getId()));
        }
    }

    public void validateProductReviewDeletionAllowed(final UUID productReviewId, final UUID currentUserId) {
        var review = productReviewRepository.findById(productReviewId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Product's review with productReviewId = '%s' was not found", productReviewId)));
        if (!currentUserId.equals(review.getUser().getId())) {
            throw new BadRequestException(String.format(
                    "Deletion of the product's review with productReviewId = '%s' is denied for the user with userId = '%s'",
                    productReviewId, currentUserId));
        }
    }

    public void validateProductIdIsValid(final UUID productId, final UUID productReviewId) {
        if (!productReviewProductGateway.exists(productId)) {
            throw new NotFoundException(String.format(
                    "Product with productId = '%s' was not found. Product's review operations (update, delete, provide) are not possible.",
                    productId));
        }
        if (!productReviewRepository.existsByIdAndProductId(productReviewId, productId)) {
            throw new NotFoundException(String.format(
                    "Product's review with productReviewId = '%s' was not found", productReviewId));
        }
    }
}

package com.zufar.icedlatte.review.validator;

import com.zufar.icedlatte.product.api.ProductReviewProductGateway;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.exception.DeniedProductReviewCreationException;
import com.zufar.icedlatte.review.exception.DeniedProductReviewDeletionException;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
import com.zufar.icedlatte.review.exception.InvalidProductReviewTextException;
import com.zufar.icedlatte.review.exception.ProductNotFoundForReviewException;
import com.zufar.icedlatte.review.exception.ProductReviewNotFoundException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewValidator unit tests")
class ProductReviewValidatorTest {

    @Mock
    private ProductReviewRepository productReviewRepository;
    @Mock
    private ProductReviewProductGateway productReviewProductGateway;
    @InjectMocks
    private ProductReviewValidator validator;

    // ── validateReviewText ──────────────────────────────────────────────────

    @Test
    @DisplayName("validateReviewText: non-blank text passes")
    void validateReviewTextNonBlankNoException() {
        assertThatCode(() -> validator.validateReviewText("Great coffee!")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateReviewText: blank text throws EmptyProductReviewException")
    void validateReviewTextBlankThrowsEmptyProductReviewException() {
        assertThatThrownBy(() -> validator.validateReviewText("   "))
                .isInstanceOf(EmptyProductReviewException.class);
    }

    @Test
    @DisplayName("validateReviewText: empty string throws EmptyProductReviewException")
    void validateReviewTextEmptyThrowsEmptyProductReviewException() {
        assertThatThrownBy(() -> validator.validateReviewText(""))
                .isInstanceOf(EmptyProductReviewException.class);
    }

    @Test
    @DisplayName("validateReviewText: text with forbidden characters throws InvalidProductReviewTextException")
    void validateReviewTextForbiddenCharsThrowsInvalidProductReviewTextException() {
        assertThatThrownBy(() -> validator.validateReviewText("Bad <script>"))
                .isInstanceOf(InvalidProductReviewTextException.class);
    }

    // ── validateProductExists ───────────────────────────────────────────────

    @Test
    @DisplayName("validateProductExists: existing product passes")
    void validateProductExistsProductFoundNoException() {
        UUID productId = UUID.randomUUID();
        when(productReviewProductGateway.exists(productId)).thenReturn(true);

        assertThatCode(() -> validator.validateProductExists(productId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateProductExists: missing product throws ProductNotFoundForReviewException")
    void validateProductExistsProductNotFoundThrowsProductNotFoundForReviewException() {
        UUID productId = UUID.randomUUID();
        when(productReviewProductGateway.exists(productId)).thenReturn(false);

        assertThatThrownBy(() -> validator.validateProductExists(productId))
                .isInstanceOf(ProductNotFoundForReviewException.class);
    }

    // ── validateReviewExistsForUser ─────────────────────────────────────────

    @Test
    @DisplayName("validateReviewExistsForUser: no existing review passes")
    void validateReviewExistsForUserNoReviewNoException() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productReviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

        assertThatCode(() -> validator.validateReviewExistsForUser(userId, productId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateReviewExistsForUser: existing review throws DeniedProductReviewCreationException")
    void validateReviewExistsForUserReviewExistsThrowsDeniedCreationException() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductReview existing = ProductReview.builder().id(UUID.randomUUID()).build();
        when(productReviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> validator.validateReviewExistsForUser(userId, productId))
                .isInstanceOf(DeniedProductReviewCreationException.class);
    }

    // ── validateProductReviewDeletionAllowed ────────────────────────────────

    @Test
    @DisplayName("validateProductReviewDeletionAllowed: owner can delete")
    void validateProductReviewDeletionAllowedOwnerDeletesNoException() {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UserEntity owner = UserEntity.builder().id(userId).build();
        ProductReview review = ProductReview.builder().id(reviewId).user(owner).build();

        when(productReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatCode(() -> validator.validateProductReviewDeletionAllowed(reviewId, userId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateProductReviewDeletionAllowed: non-owner throws DeniedProductReviewDeletionException")
    void validateProductReviewDeletionAllowedNonOwnerThrowsDeniedDeletionException() {
        UUID currentUserId = UUID.randomUUID();
        UUID reviewOwnerId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UserEntity owner = UserEntity.builder().id(reviewOwnerId).build();
        ProductReview review = ProductReview.builder().id(reviewId).user(owner).build();

        when(productReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> validator.validateProductReviewDeletionAllowed(reviewId, currentUserId))
                .isInstanceOf(DeniedProductReviewDeletionException.class);
    }

    // ── validateProductIdIsValid ────────────────────────────────────────────

    @Test
    @DisplayName("validateProductIdIsValid: review belongs to product passes")
    void validateProductIdIsValidReviewBelongsToProductNoException() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(productReviewProductGateway.exists(productId)).thenReturn(true);
        when(productReviewRepository.existsByIdAndProductId(reviewId, productId)).thenReturn(true);

        assertThatCode(() -> validator.validateProductIdIsValid(productId, reviewId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateProductIdIsValid: product missing throws ProductNotFoundForReviewException")
    void validateProductIdIsValidProductMissingThrowsProductNotFoundForReviewException() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(productReviewProductGateway.exists(productId)).thenReturn(false);

        assertThatThrownBy(() -> validator.validateProductIdIsValid(productId, reviewId))
                .isInstanceOf(ProductNotFoundForReviewException.class);
    }

    @Test
    @DisplayName("validateProductIdIsValid: review missing throws ProductReviewNotFoundException")
    void validateProductIdIsValidReviewMissingThrowsProductReviewNotFoundException() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(productReviewProductGateway.exists(productId)).thenReturn(true);
        when(productReviewRepository.existsByIdAndProductId(reviewId, productId)).thenReturn(false);

        assertThatThrownBy(() -> validator.validateProductIdIsValid(productId, reviewId))
                .isInstanceOf(ProductReviewNotFoundException.class);
    }

    @Test
    @DisplayName("validateProductIdIsValid: review exists but belongs to different product throws ProductReviewNotFoundException")
    void validateProductIdIsValidReviewBelongsToDifferentProductThrowsProductReviewNotFoundException() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(productReviewProductGateway.exists(productId)).thenReturn(true);
        when(productReviewRepository.existsByIdAndProductId(reviewId, productId)).thenReturn(false);

        assertThatThrownBy(() -> validator.validateProductIdIsValid(productId, reviewId))
                .isInstanceOf(ProductReviewNotFoundException.class);
    }
}

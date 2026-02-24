package com.zufar.icedlatte.review.validator;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.api.ProductReviewProvider;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.exception.DeniedProductReviewCreationException;
import com.zufar.icedlatte.review.exception.DeniedProductReviewDeletionException;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
import com.zufar.icedlatte.review.exception.ProductNotFoundForReviewException;
import com.zufar.icedlatte.review.exception.ProductReviewNotFoundException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
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
    private SecurityPrincipalProvider securityPrincipalProvider;
    @Mock
    private ProductReviewProvider productReviewProvider;
    @Mock
    private ProductReviewRepository productReviewRepository;
    @Mock
    private ProductInfoRepository productInfoRepository;
    @InjectMocks
    private ProductReviewValidator validator;

    // ── validateReviewText ──────────────────────────────────────────────────

    @Test
    @DisplayName("validateReviewText: non-blank text passes")
    void validateReviewText_nonBlank_noException() {
        assertThatCode(() -> validator.validateReviewText("Great coffee!")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateReviewText: blank text throws EmptyProductReviewException")
    void validateReviewText_blank_throwsEmptyProductReviewException() {
        assertThatThrownBy(() -> validator.validateReviewText("   "))
                .isInstanceOf(EmptyProductReviewException.class);
    }

    @Test
    @DisplayName("validateReviewText: empty string throws EmptyProductReviewException")
    void validateReviewText_empty_throwsEmptyProductReviewException() {
        assertThatThrownBy(() -> validator.validateReviewText(""))
                .isInstanceOf(EmptyProductReviewException.class);
    }

    // ── validateProductExists ───────────────────────────────────────────────

    @Test
    @DisplayName("validateProductExists: existing product passes")
    void validateProductExists_productFound_noException() {
        UUID productId = UUID.randomUUID();
        when(productInfoRepository.findById(productId)).thenReturn(Optional.of(new com.zufar.icedlatte.product.entity.ProductInfo()));

        assertThatCode(() -> validator.validateProductExists(productId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateProductExists: missing product throws ProductNotFoundForReviewException")
    void validateProductExists_productNotFound_throwsProductNotFoundForReviewException() {
        UUID productId = UUID.randomUUID();
        when(productInfoRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validateProductExists(productId))
                .isInstanceOf(ProductNotFoundForReviewException.class);
    }

    // ── validateReviewExistsForUser ─────────────────────────────────────────

    @Test
    @DisplayName("validateReviewExistsForUser: no existing review passes")
    void validateReviewExistsForUser_noReview_noException() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productReviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

        assertThatCode(() -> validator.validateReviewExistsForUser(userId, productId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateReviewExistsForUser: existing review throws DeniedProductReviewCreationException")
    void validateReviewExistsForUser_reviewExists_throwsDeniedCreationException() {
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
    void validateProductReviewDeletionAllowed_ownerDeletes_noException() {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UserEntity owner = UserEntity.builder().id(userId).build();
        ProductReview review = ProductReview.builder().id(reviewId).user(owner).build();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(productReviewProvider.getReviewEntityById(reviewId)).thenReturn(review);

        assertThatCode(() -> validator.validateProductReviewDeletionAllowed(reviewId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateProductReviewDeletionAllowed: non-owner throws DeniedProductReviewDeletionException")
    void validateProductReviewDeletionAllowed_nonOwner_throwsDeniedDeletionException() {
        UUID currentUserId = UUID.randomUUID();
        UUID reviewOwnerId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UserEntity owner = UserEntity.builder().id(reviewOwnerId).build();
        ProductReview review = ProductReview.builder().id(reviewId).user(owner).build();

        when(securityPrincipalProvider.getUserId()).thenReturn(currentUserId);
        when(productReviewProvider.getReviewEntityById(reviewId)).thenReturn(review);

        assertThatThrownBy(() -> validator.validateProductReviewDeletionAllowed(reviewId))
                .isInstanceOf(DeniedProductReviewDeletionException.class);
    }

    // ── validateProductIdIsValid ────────────────────────────────────────────

    @Test
    @DisplayName("validateProductIdIsValid: both exist passes")
    void validateProductIdIsValid_bothExist_noException() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(productInfoRepository.existsById(productId)).thenReturn(true);
        when(productReviewRepository.existsById(reviewId)).thenReturn(true);

        assertThatCode(() -> validator.validateProductIdIsValid(productId, reviewId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateProductIdIsValid: product missing throws ProductNotFoundForReviewException")
    void validateProductIdIsValid_productMissing_throwsProductNotFoundForReviewException() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(productInfoRepository.existsById(productId)).thenReturn(false);

        assertThatThrownBy(() -> validator.validateProductIdIsValid(productId, reviewId))
                .isInstanceOf(ProductNotFoundForReviewException.class);
    }

    @Test
    @DisplayName("validateProductIdIsValid: review missing throws ProductReviewNotFoundException")
    void validateProductIdIsValid_reviewMissing_throwsProductReviewNotFoundException() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        when(productInfoRepository.existsById(productId)).thenReturn(true);
        when(productReviewRepository.existsById(reviewId)).thenReturn(false);

        assertThatThrownBy(() -> validator.validateProductIdIsValid(productId, reviewId))
                .isInstanceOf(ProductReviewNotFoundException.class);
    }
}

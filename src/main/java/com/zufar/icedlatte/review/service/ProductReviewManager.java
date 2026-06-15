package com.zufar.icedlatte.review.service;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.turnstile.TurnstileProperties;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.product.api.ProductReviewProductApi;
import com.zufar.icedlatte.review.api.ReviewMaintenanceApi;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.entity.ProductReviewLike;
import com.zufar.icedlatte.review.exception.ReviewConflictException;
import com.zufar.icedlatte.review.exception.ReviewNotFoundException;
import com.zufar.icedlatte.review.repository.ProductReviewLikeRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.service.ai.summary.ProductReviewSummaryDebouncer;
import com.zufar.icedlatte.review.service.validator.ProductReviewValidator;
import com.zufar.icedlatte.user.api.UserLookupApi;
import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductReviewManager implements ReviewMaintenanceApi {

    private final ProductReviewRepository reviewRepository;
    private final ProductReviewLikeRepository productReviewLikeRepository;
    private final ProductReviewDtoConverter productReviewDtoConverter;
    private final UserLookupApi userLookupApi;
    private final ProductReviewValidator productReviewValidator;
    private final ProductReviewProductApi productReviewProductApi;
    private final ProductReviewSummaryDebouncer summaryDebouncer;
    private final ApplicationEventPublisher eventPublisher;
    private final TurnstileVerifier turnstileVerifier;
    private final TurnstileProperties turnstileProperties;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ProductReviewDto create(
            final UUID productId, final UUID userId, final @Nullable ProductReviewRequest productReviewRequest) {
        ProductReviewRequest reviewRequest = Optional.ofNullable(productReviewRequest)
                .orElseThrow(() -> new BadRequestException("Product's review request must be provided"));
        if (turnstileProperties.reviewsEnabled()) {
            turnstileVerifier.verify(reviewRequest.getTurnstileToken());
        }
        var productReviewText = reviewRequest.getText();

        productReviewValidator.validateProductExists(productId);
        productReviewValidator.validateReviewText(productReviewText);
        productReviewValidator.validateProductRating(reviewRequest.getRating());
        productReviewValidator.validateReviewExistsForUser(userId, productId);

        var user = userLookupApi.getUserById(userId);
        var productReview = buildProductReview(productId, userId, reviewRequest);

        try {
            reviewRepository.saveAndFlush(productReview);
        } catch (DataIntegrityViolationException e) {
            String errorMessage =
                    "Creation of the product's review for the user with userId = '%s' and the product with productId = '%s' is denied. Delete the previous product's review first.";
            throw new ReviewConflictException(String.format(errorMessage, userId, productId), e);
        }
        refreshReviewState(productId);

        eventPublisher.publishEvent(new ReviewCreatedEvent(productReview.getId(), productId));

        return productReviewDtoConverter.toProductReviewDto(productReview, user);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID productId, final UUID productReviewId, final UUID userId) {
        productReviewValidator.validateProductReviewDeletionAllowed(productReviewId, userId);
        productReviewValidator.validateProductIdIsValid(productId, productReviewId);

        reviewRepository.deleteById(productReviewId);

        refreshReviewState(productId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ProductReviewDto updateLike(
            final UUID productId,
            final UUID productReviewId,
            final UUID userId,
            final @Nullable Boolean newProductReviewLike) {
        if (newProductReviewLike == null) {
            String errorMessage =
                    "GetReviewsRequest parameters are incorrect. Error messages are [ Review vote 'isLike' must be provided. ].";
            throw new BadRequestException(errorMessage);
        }
        productReviewValidator.validateProductIdIsValid(productId, productReviewId);

        upsertProductReviewLike(userId, productId, productReviewId, newProductReviewLike);

        reviewRepository.updateLikesCount(productReviewId);
        reviewRepository.updateDislikesCount(productReviewId);

        ProductReview productReview = reviewRepository
                .findById(productReviewId)
                .orElseThrow(() -> new ReviewNotFoundException(productReviewId));
        UserLookupSnapshot user = userLookupApi.getUserById(productReview.getUserId());
        return productReviewDtoConverter.toProductReviewDto(productReview, user);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void refreshAllCounts() {
        reviewRepository.updateAllLikesCounts();
        reviewRepository.updateAllDislikesCounts();
    }

    private ProductReview buildProductReview(UUID productId, UUID userId, ProductReviewRequest reviewRequest) {
        return ProductReview.builder()
                .userId(userId)
                .productId(productId)
                .text(reviewRequest.getText().trim())
                .productRating(reviewRequest.getRating())
                .likesCount(0)
                .dislikesCount(0)
                .build();
    }

    private void refreshReviewState(UUID productId) {
        productReviewProductApi.refreshReviewAggregates(productId);
        summaryDebouncer.schedule(productId);
    }

    private void upsertProductReviewLike(
            UUID userId, UUID productId, UUID productReviewId, Boolean newProductReviewLike) {
        productReviewLikeRepository
                .findByUserIdAndProductReviewId(userId, productReviewId)
                .ifPresentOrElse(
                        entity -> updateExistingProductReviewLike(entity, newProductReviewLike),
                        () -> createProductReviewLike(userId, productId, productReviewId, newProductReviewLike));
    }

    private void updateExistingProductReviewLike(ProductReviewLike entity, Boolean newProductReviewLike) {
        if (entity.getIsLike().equals(newProductReviewLike)) {
            return;
        }
        entity.setIsLike(newProductReviewLike);
        productReviewLikeRepository.saveAndFlush(entity);
    }

    private void createProductReviewLike(
            UUID userId, UUID productId, UUID productReviewId, Boolean newProductReviewLike) {
        ProductReviewLike newReviewLike = ProductReviewLike.builder()
                .userId(userId)
                .productId(productId)
                .productReviewId(productReviewId)
                .isLike(newProductReviewLike)
                .build();
        try {
            productReviewLikeRepository.saveAndFlush(newReviewLike);
        } catch (DataIntegrityViolationException e) {
            String errorMessage = "Product review vote could not be recorded because it was changed concurrently.";
            throw new ReviewConflictException(errorMessage, e);
        }
    }
}

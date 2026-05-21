package com.zufar.icedlatte.review.service;

import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.product.api.ProductReviewProductApi;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.service.ai.summary.ProductReviewSummaryDebouncer;
import com.zufar.icedlatte.review.service.validator.ProductReviewValidator;
import com.zufar.icedlatte.user.api.UserLookupApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductReviewManager {

    private final ProductReviewRepository reviewRepository;
    private final ProductReviewDtoConverter productReviewDtoConverter;
    private final UserLookupApi userLookupApi;
    private final ProductReviewValidator productReviewValidator;
    private final ProductReviewProductApi productReviewProductApi;
    private final ProductReviewSummaryDebouncer summaryDebouncer;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ProductReviewDto create(final UUID productId,
                                   final UUID userId,
                                   final ProductReviewRequest productReviewRequest) {
        var productReviewText = productReviewRequest.getText();

        productReviewValidator.validateProductExists(productId);
        productReviewValidator.validateReviewText(productReviewText);
        productReviewValidator.validateReviewExistsForUser(userId, productId);

        var user = userLookupApi.getUserById(userId);
        var productReview = ProductReview.builder()
                .userId(userId)
                .productId(productId)
                .text(productReviewText.trim())
                .productRating(productReviewRequest.getRating())
                .likesCount(0)
                .dislikesCount(0)
                .build();

        reviewRepository.saveAndFlush(productReview);
        summaryDebouncer.schedule(productId);

        productReviewProductApi.refreshReviewAggregates(productId);

        eventPublisher.publishEvent(new ReviewCreatedEvent(productReview.getId(), productReviewText.trim(), productId));

        return productReviewDtoConverter.toProductReviewDto(productReview, user);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID productId,
                       final UUID productReviewId,
                       final UUID userId) {
        productReviewValidator.validateProductReviewDeletionAllowed(productReviewId, userId);
        productReviewValidator.validateProductIdIsValid(productId, productReviewId);

        reviewRepository.deleteById(productReviewId);

        productReviewProductApi.refreshReviewAggregates(productId);

        summaryDebouncer.schedule(productId);
    }
}

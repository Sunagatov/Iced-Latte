package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.review.validator.GetReviewsRequestValidator;
import com.zufar.icedlatte.review.api.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.zufar.icedlatte.common.util.Utils.createPageableObject;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = ProductReviewEndpoint.PRODUCT_REVIEW_URL)
public class ProductReviewEndpoint implements com.zufar.icedlatte.openapi.product.review.api.ProductReviewApi {

    public static final String PRODUCT_REVIEW_URL = "/api/v1/products/";

    private final ProductReviewCreator productReviewCreator;
    private final ProductReviewDeleter productReviewDeleter;
    private final ProductReviewsProvider productReviewsProvider;
    private final ProductReviewsStatisticsProvider productReviewsStatisticsProvider;
    private final ProductReviewLikesUpdater productReviewLikesUpdater;
    private final GetReviewsRequestValidator getReviewsRequestValidator;
    private final PaginationConfig paginationConfig;

    @Override
    @PostMapping(value = "/{productId}/reviews")
    public ResponseEntity<ProductReviewDto> addNewProductReview(@PathVariable final UUID productId,
                                                                @Valid @RequestBody final ProductReviewRequest productReviewRequest) {
        log.info("Adding product review for productId: {}", productId);
        var review = productReviewCreator.create(productId, productReviewRequest);
        log.info("Product review created with id: {} for productId: {}", review.getProductReviewId(), productId);
        return ResponseEntity.ok(review);
    }

    @Override
    @DeleteMapping(value = "/{productId}/reviews/{productReviewId}")
    public ResponseEntity<Void> deleteProductReview(@PathVariable final UUID productId,
                                                    @PathVariable final UUID productReviewId) {
        log.info("Deleting product review: {} for productId: {}", productReviewId, productId);
        productReviewDeleter.delete(productId, productReviewId);
        log.info("Product review deleted: {}", productReviewId);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping(value = "/{productId}/reviews")
    public ResponseEntity<ProductReviewsAndRatingsWithPagination> getProductReviewsAndRatings(@PathVariable final UUID productId,
                                                                                              @RequestParam(name = "page", required = false) final Integer pageNumber,
                                                                                              @RequestParam(name = "size", required = false) final Integer pageSize,
                                                                                              @RequestParam(name = "sort_attribute", required = false) final String sortAttribute,
                                                                                              @RequestParam(name = "sort_direction", required = false) final String sortDirection,
                                                                                              @RequestParam(name = "product_ratings", required = false) List<Integer> productRatings) {
        // Apply default values from configuration
        Integer finalPageNumber = pageNumber != null ? pageNumber : paginationConfig.getDefaultPageNumber();
        Integer finalPageSize = pageSize != null ? pageSize : paginationConfig.getReviews().getDefaultPageSize();
        String finalSortAttribute = sortAttribute != null ? sortAttribute : paginationConfig.getReviews().getDefaultSortAttribute();
        String finalSortDirection = sortDirection != null ? sortDirection : paginationConfig.getReviews().getDefaultSortDirection();
        
        log.info("Getting reviews for productId: {} with pagination: page={}, size={}", productId, finalPageNumber, finalPageSize);
        getReviewsRequestValidator.validate(finalPageNumber, finalPageSize, finalSortAttribute, finalSortDirection, productRatings);
        Pageable pageable = createPageableObject(finalPageNumber, finalPageSize, finalSortAttribute, finalSortDirection);
        var reviews = productReviewsProvider.getProductReviews(productId, pageable, productRatings);
        log.info("Retrieved {} reviews for productId: {}", reviews.getReviewsWithRatings().size(), productId);
        return ResponseEntity.ok(reviews);
    }

    @Override
    @GetMapping(value = "/{productId}/review")
    public ResponseEntity<ProductReviewDto> getProductReview(@PathVariable final UUID productId) {
        // Validate UUID input to prevent code injection
        if (productId == null) {
            log.warn("Invalid product ID: null value received");
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Getting user review for productId: {}", productId);
        var result = productReviewsProvider.getProductReviewForUser(productId);
        log.info("User review retrieved for productId: {}", productId);
        return ResponseEntity.ok(result);
    }

    @Override
    @GetMapping("/{productId}/reviews/statistics")
    public ResponseEntity<ProductReviewRatingStats> getRatingAndReviewStat(@PathVariable final UUID productId) {
        log.info("Getting review statistics for productId: {}", productId);
        var stats = productReviewsStatisticsProvider.get(productId);
        log.info("Review statistics retrieved for productId: {}", productId);
        return ResponseEntity.ok(stats);
    }

    @Override
    @PostMapping(value = "/{productId}/reviews/{productReviewId}/likes")
    public ResponseEntity<ProductReviewDto> addProductReviewLike(@PathVariable @Validated final UUID productId,
                                                                 @PathVariable @Validated final UUID productReviewId,
                                                                 @Valid @RequestBody final ProductReviewLikeDto request) {
        // Validate UUID inputs to prevent code injection
        if (productId == null || productReviewId == null) {
            log.warn("Invalid UUID parameters: productId={}, productReviewId={}", productId, productReviewId);
            return ResponseEntity.badRequest().build();
        }
        
        log.info("Rating review: {} for productId: {} as {}", productReviewId, productId, request.getIsLike() ? "like" : "dislike");
        var productReview = productReviewLikesUpdater.update(productId, productReviewId, request.getIsLike());
        log.info("Review {} successfully {}", productReviewId, request.getIsLike() ? "liked" : "disliked");
        return ResponseEntity.ok(productReview);
    }
}

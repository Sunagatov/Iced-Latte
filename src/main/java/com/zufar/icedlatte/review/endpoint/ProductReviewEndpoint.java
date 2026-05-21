package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.review.service.ProductReviewManager;
import com.zufar.icedlatte.review.service.ProductReviewsProvider;
import com.zufar.icedlatte.security.api.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class ProductReviewEndpoint implements com.zufar.icedlatte.openapi.product.review.api.ProductReviewApi {

    public static final String PRODUCT_REVIEW_URL = ApiPaths.PRODUCTS;

    private final ProductReviewManager productReviewService;
    private final ProductReviewsProvider productReviewsProvider;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @PostMapping(ApiPaths.PRODUCTS + "/{productId}/reviews")
    public ResponseEntity<ProductReviewDto> addNewProductReview(@PathVariable final UUID productId,
                                                                @Valid @RequestBody final ProductReviewRequest productReviewRequest) {
        UUID userId = currentUserProvider.getUserId();
        var review = productReviewService.create(productId, userId, productReviewRequest);
        log.info("review.created: reviewId={}, productId={}", review.getProductReviewId(), productId);
        return ResponseEntity.ok(review);
    }

    @Override
    @DeleteMapping(ApiPaths.PRODUCTS + "/{productId}/reviews/{productReviewId}")
    public ResponseEntity<Void> deleteProductReview(@PathVariable final UUID productId,
                                                    @PathVariable final UUID productReviewId) {
        UUID userId = currentUserProvider.getUserId();
        productReviewService.delete(productId, productReviewId, userId);
        log.info("review.deleted: reviewId={}", productReviewId);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping(ApiPaths.PRODUCTS + "/{productId}/reviews")
    public ResponseEntity<ProductReviewsAndRatingsWithPagination> getProductReviewsAndRatings(
            @PathVariable final UUID productId,
            @RequestParam(name = "page", required = false, defaultValue = "0") final Integer pageNumber,
            @RequestParam(name = "size", required = false) final Integer pageSize,
            @RequestParam(name = "sort_attribute", required = false) final String sortAttribute,
            @RequestParam(name = "sort_direction", required = false) final String sortDirection,
            @RequestParam(name = "productRatings", required = false) List<Integer> productRatings) {
        return ResponseEntity.ok(productReviewsProvider.getProductReviews(
                productId, pageNumber, pageSize, sortAttribute, sortDirection, productRatings));
    }

    @Override
    @GetMapping(ApiPaths.PRODUCTS + "/{productId}/review")
    public ResponseEntity<ProductReviewDto> getProductReview(@PathVariable final UUID productId) {
        return ResponseEntity.ok(productReviewsProvider.getProductReviewForUser(productId, currentUserProvider.getUserId()));
    }

    @Override
    @GetMapping(ApiPaths.PRODUCTS + "/{productId}/reviews/statistics")
    public ResponseEntity<ProductReviewRatingStats> getRatingAndReviewStat(@PathVariable final UUID productId) {
        return ResponseEntity.ok(productReviewsProvider.getStatistics(productId));
    }

    @Override
    @PostMapping(ApiPaths.PRODUCTS + "/{productId}/reviews/{productReviewId}/likes")
    public ResponseEntity<ProductReviewDto> addProductReviewLike(@PathVariable final UUID productId,
                                                                 @PathVariable final UUID productReviewId,
                                                                 @Valid @RequestBody final ProductReviewLikeDto request) {
        UUID userId = currentUserProvider.getUserId();
        var productReview = productReviewService.updateLike(productId, productReviewId, userId, request.getIsLike());
        log.info("review.rated: reviewId={}, vote={}", productReviewId, Boolean.TRUE.equals(request.getIsLike()) ? "liked" : "disliked");
        return ResponseEntity.ok(productReview);
    }

    @Override
    @GetMapping(ApiPaths.USERS + "/reviews")
    public ResponseEntity<ProductReviewsAndRatingsWithPagination> getUserReviews(
            @RequestParam(name = "page", required = false, defaultValue = "0") final Integer pageNumber,
            @RequestParam(name = "size", required = false, defaultValue = "50") final Integer pageSize,
            @RequestParam(name = "sort_attribute", required = false, defaultValue = "createdAt") final String sortAttribute,
            @RequestParam(name = "sort_direction", required = false, defaultValue = "asc") final String sortDirection) {
        return ResponseEntity.ok(productReviewsProvider.getUserReviews(
                currentUserProvider.getUserId(), pageNumber, pageSize, sortAttribute, sortDirection));
    }
}

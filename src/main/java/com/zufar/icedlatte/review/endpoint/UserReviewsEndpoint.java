package com.zufar.icedlatte.review.endpoint;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.openapi.product.review.api.UserReviewsApi;
import com.zufar.icedlatte.review.service.ProductReviewManager;
import com.zufar.icedlatte.review.service.ProductReviewsProvider;
import com.zufar.icedlatte.security.api.CurrentUserProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class UserReviewsEndpoint implements UserReviewsApi {

    private final ProductReviewManager productReviewService;
    private final ProductReviewsProvider productReviewsProvider;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @PostMapping(ApiPaths.PRODUCTS + "/{productId}/reviews")
    public ResponseEntity<ProductReviewDto> addNewProductReview(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductReviewRequest productReviewRequest) {
        UUID userId = currentUserProvider.getUserId();
        var review = productReviewService.create(productId, userId, productReviewRequest);
        log.info("review.created: reviewId={}, productId={}", review.getProductReviewId(), productId);
        return ResponseEntity.ok(review);
    }

    @Override
    @DeleteMapping(ApiPaths.PRODUCTS + "/{productId}/reviews/{productReviewId}")
    public ResponseEntity<Void> deleteProductReview(@PathVariable UUID productId, @PathVariable UUID productReviewId) {
        UUID userId = currentUserProvider.getUserId();
        productReviewService.delete(productId, productReviewId, userId);
        log.info("review.deleted: reviewId={}", productReviewId);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping(ApiPaths.PRODUCTS + "/{productId}/review")
    public ResponseEntity<ProductReviewDto> getProductReview(@PathVariable UUID productId) {
        return ResponseEntity.ok(
                productReviewsProvider.getProductReviewForUser(productId, currentUserProvider.getUserId()));
    }

    @Override
    @GetMapping(ApiPaths.USERS + "/reviews")
    public ResponseEntity<ProductReviewsAndRatingsWithPagination> getUserReviews(
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer pageNumber,
            @RequestParam(name = "size", required = false, defaultValue = "50") Integer pageSize,
            @RequestParam(name = "sort_attribute", required = false, defaultValue = "createdAt")
                    String sortAttribute,
            @RequestParam(name = "sort_direction", required = false, defaultValue = "asc") String sortDirection) {
        return ResponseEntity.ok(productReviewsProvider.getUserReviews(
                currentUserProvider.getUserId(), pageNumber, pageSize, sortAttribute, sortDirection));
    }
}

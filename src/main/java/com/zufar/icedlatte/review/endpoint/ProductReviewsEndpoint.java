package com.zufar.icedlatte.review.endpoint;

import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nullable;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.ProductReviewRatingStats;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.openapi.product.review.api.ProductReviewsApi;
import com.zufar.icedlatte.review.service.ProductReviewsProvider;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
public class ProductReviewsEndpoint implements ProductReviewsApi {

    public static final String PRODUCT_REVIEWS_URL = ApiPaths.PRODUCTS;

    private final ProductReviewsProvider productReviewsProvider;

    @Override
    @GetMapping(ApiPaths.PRODUCTS + "/{productId}/reviews")
    public ResponseEntity<ProductReviewsAndRatingsWithPagination> getProductReviewsAndRatings(
            @PathVariable UUID productId,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer pageNumber,
            @RequestParam(name = "size", required = false) Integer pageSize,
            @RequestParam(name = "sort_attribute", required = false) String sortAttribute,
            @RequestParam(name = "sort_direction", required = false) String sortDirection,
            @Nullable @RequestParam(name = "productRatings", required = false) List<Integer> productRatings) {
        return ResponseEntity.ok(productReviewsProvider.getProductReviews(
                productId, pageNumber, pageSize, sortAttribute, sortDirection, productRatings));
    }

    @Override
    @GetMapping(ApiPaths.PRODUCTS + "/{productId}/reviews/statistics")
    public ResponseEntity<ProductReviewRatingStats> getRatingAndReviewStat(@PathVariable UUID productId) {
        return ResponseEntity.ok(productReviewsProvider.getStatistics(productId));
    }
}

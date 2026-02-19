package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewLikeDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewRatingStats;
import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.review.api.ProductReviewCreator;
import com.zufar.icedlatte.review.api.ProductReviewDeleter;
import com.zufar.icedlatte.review.api.ProductReviewLikesUpdater;
import com.zufar.icedlatte.review.api.ProductReviewsProvider;
import com.zufar.icedlatte.review.api.ProductReviewsStatisticsProvider;
import com.zufar.icedlatte.review.validator.GetReviewsRequestValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
    public ResponseEntity<ProductReviewsAndRatingsWithPagination> getProductReviewsAndRatings(
            @PathVariable final UUID productId,
            @RequestParam(name = "page", required = false) final Integer pageNumber,
            @RequestParam(name = "size", required = false) final Integer pageSize,
            @RequestParam(name = "sort_attribute", required = false) final String sortAttribute,
            @RequestParam(name = "sort_direction", required = false) final String sortDirection,
            @RequestParam(name = "product_ratings", required = false) List<Integer> productRatings) {
        getReviewsRequestValidator.validate(pageNumber, pageSize, sortAttribute, sortDirection, productRatings);
        return ResponseEntity.ok(productReviewsProvider.getProductReviews(
                productId, pageNumber, pageSize, sortAttribute, sortDirection, productRatings));
    }

    @Override
    @GetMapping(value = "/{productId}/review")
    public ResponseEntity<ProductReviewDto> getProductReview(@PathVariable final UUID productId) {
        return ResponseEntity.ok(productReviewsProvider.getProductReviewForUser(productId));
    }

    @Override
    @GetMapping("/{productId}/reviews/statistics")
    public ResponseEntity<ProductReviewRatingStats> getRatingAndReviewStat(@PathVariable final UUID productId) {
        return ResponseEntity.ok(productReviewsStatisticsProvider.get(productId));
    }

    @Override
    @PostMapping(value = "/{productId}/reviews/{productReviewId}/likes")
    public ResponseEntity<ProductReviewDto> addProductReviewLike(@PathVariable @Validated final UUID productId,
                                                                 @PathVariable @Validated final UUID productReviewId,
                                                                 @Valid @RequestBody final ProductReviewLikeDto request) {
        log.info("Rating review: {} for productId: {} as {}", productReviewId, productId, request.getIsLike() ? "like" : "dislike");
        var productReview = productReviewLikesUpdater.update(productId, productReviewId, request.getIsLike());
        log.info("Review {} successfully {}", productReviewId, request.getIsLike() ? "liked" : "disliked");
        return ResponseEntity.ok(productReview);
    }
}

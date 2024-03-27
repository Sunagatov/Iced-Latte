package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.openapi.dto.ProductReviewResponse;
import com.zufar.icedlatte.openapi.dto.ProductReviewWithRating;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.review.api.ProductReviewsAndRatingsProvider;
import com.zufar.icedlatte.review.api.ProductReviewCreator;
import com.zufar.icedlatte.review.api.ProductReviewDeleter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    private final ProductReviewsAndRatingsProvider productReviewsAndRatingsProvider;

    @Override
    @PostMapping(value = "/{productId}/reviews")
    public ResponseEntity<ProductReviewResponse> addNewProductReview(@PathVariable final UUID productId,
                                                           final ProductReviewRequest productReviewRequest) {
        log.info("Received request to add product review for product with productId = '{}'", productId);
        var review = productReviewCreator.create(productId, productReviewRequest);
        log.info("Product review was added with productReviewId = '{}'", review.getProductReviewId());
        return ResponseEntity.ok().body(review);
    }

    @Override
    @DeleteMapping(value = "/{productId}/reviews/{productReviewId}")
    public ResponseEntity<Void> deleteProductReview(@PathVariable final UUID productId,
                                             @PathVariable final UUID productReviewId) {
        log.info("Received request to delete product review with productReviewId = '{}', productId = '{}'", productReviewId, productId);
        productReviewDeleter.delete(productId, productReviewId);
        log.info("Product review was deleted");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @GetMapping(value = "/{productId}/reviews")
    public ResponseEntity<ProductReviewsAndRatingsWithPagination> getProductReviewsAndRatings(@PathVariable final UUID productId,
                                                                                       @RequestParam(name = "page", defaultValue = "0") final Integer page,
                                                                                       @RequestParam(name = "size", defaultValue = "10") final Integer size,
                                                                                       @RequestParam(name = "sort_attribute", defaultValue = "createdAt") final String sortAttribute,
                                                                                       @RequestParam(name = "sort_direction", defaultValue = "desc") final String sortDirection) {
        log.info("Received the request to get reviews and ratings for product {} with these pagination and sorting attributes: page - {}, size - {}, sort_attribute - {}, sort_direction - {}",
                productId, page, size, sortAttribute, sortDirection);
        ProductReviewsAndRatingsWithPagination reviewsPaginationDto = productReviewsAndRatingsProvider.getProductReviews(productId, page, size, sortAttribute, sortDirection);
        log.info("Product reviews and ratings were retrieved successfully");
        return ResponseEntity.ok().body(reviewsPaginationDto);
    }

    @Override
    @GetMapping(value = "/{productId}/review")
    public ResponseEntity<ProductReviewWithRating> getProductReview(@PathVariable final UUID productId){
        log.info("Received the request to get product review and rating for product {}", productId);
        ProductReviewWithRating result = productReviewsAndRatingsProvider.getProductReviewAndRatingByUser(productId);
        log.info("Product review and rating were retrieved successfully");
        return ResponseEntity.ok().body(result);
    }
}

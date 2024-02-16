package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.openapi.dto.NewReview;
import com.zufar.icedlatte.openapi.dto.ReviewResponse;
import com.zufar.icedlatte.review.api.ReviewManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = ReviewEndpoint.REVIEW_URL)
public class ReviewEndpoint implements com.zufar.icedlatte.openapi.review.api.ReviewApi {

    public static final String REVIEW_URL = "/api/v1/products/";

    private final ReviewManager reviewManager;

    @Override
    @PostMapping(value = "/{productId}/reviews")
    public ResponseEntity<ReviewResponse> addReview(@PathVariable final UUID productId, final NewReview newReview) {
        log.info("Received request to add review for product with id: {}", productId);
        var review = reviewManager.create(productId, newReview);
        log.info("Review was added with id: {}", review.getReviewId());
        return ResponseEntity.ok().body(review);
    }

    @Override
    @DeleteMapping(value = "/{productId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable final UUID productId, @PathVariable final UUID reviewId) {
        log.info("Received request to delete review with id: {}, product id: {}", reviewId, productId);
        reviewManager.delete(reviewId);
        log.info("Review was deleted");
        return new ResponseEntity<>(HttpStatus.OK);
    }
}

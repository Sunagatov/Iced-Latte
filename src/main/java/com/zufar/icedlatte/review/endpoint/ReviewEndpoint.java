package com.zufar.icedlatte.review.endpoint;

import com.zufar.icedlatte.openapi.dto.NewReview;
import com.zufar.icedlatte.openapi.dto.ReviewResponse;
import com.zufar.icedlatte.review.api.ReviewManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
        log.info("Received request to add review.");
        var review = reviewManager.createReview(productId, newReview);
        log.info("Review was added with id: {}", review.getReviewId());
        return ResponseEntity.ok().body(review);
    }

}

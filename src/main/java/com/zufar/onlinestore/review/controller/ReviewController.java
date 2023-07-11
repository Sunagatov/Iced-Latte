package com.zufar.onlinestore.review.controller;

import com.zufar.onlinestore.review.dto.ReviewDto;
import com.zufar.onlinestore.review.exception.ReviewNotFoundException;
import com.zufar.onlinestore.review.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(ReviewController.REVIEW_URL)
@Validated
@RequiredArgsConstructor
public class ReviewController {

    public static final String REVIEW_URL = "/api/products";
    private final ReviewService reviewService;

    @PostMapping("/{productId}/reviews")
    public ResponseEntity<ApiResponse<ReviewDto>> addReview(@PathVariable String productId,
                                                            @RequestBody @Valid @NotNull(message = "Request body is mandatory") ReviewDto request) {
        log.info("Received request to create Review - {}.", request);
        request.setProductId(productId);
        ReviewDto savedReviewDto = reviewService.addReview(request);
        log.info("[{}] Review with ID {} was created for product {}.", LocalDateTime.now(), savedReviewDto.getId(), productId);

        ApiResponse<ReviewDto> apiResponse = ApiResponse.<ReviewDto>builder()
                .data(savedReviewDto)
                .message("Review is added")
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/{productId}/reviews")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> getProductReviews(@PathVariable String productId) {
        log.info("Received request to get all Reviews for Product Id {}", productId);
        List<ReviewDto> reviewDtos = reviewService.getProductReviews(productId);
        if (reviewDtos.isEmpty()) {
            log.info("Reviews for product Id {} are absent.", productId);
            return ResponseEntity.notFound().build();
        }
        log.info("All Reviews for Product - {} were retrieved - {}.", productId, reviewDtos);

        ApiResponse<List<ReviewDto>> apiResponse = ApiResponse.<List<ReviewDto>>builder()
                .data(reviewDtos)
                .message("Reviews retrieved successfully")
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .build();

        return ResponseEntity.ok().body(apiResponse);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<String>> deleteReview(@PathVariable Long reviewId) {
        log.info("Received request to delete the Review with id - {}.", reviewId);
        reviewService.deleteReview(reviewId);
        log.info("The Review with id - {} was deleted.", reviewId);

        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .data(String.valueOf(reviewId))
                .message(String.format("The Review with id %s was deleted.", reviewId))
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewDto>> editReview(@PathVariable Long reviewId,
                                                             @RequestBody @Valid @NotNull(message = "Request body is mandatory") ReviewDto request) {
        log.info("Received request to edit the review with id - {}, request - {}.", reviewId, request);

        ReviewDto existingReviewDto = reviewService.findReview(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        existingReviewDto.setText(request.getText());
        existingReviewDto.setRating(request.getRating());

        ReviewDto updatedReviewDto = reviewService.addReview(existingReviewDto);

        log.info("The review with id - {} was edited.", updatedReviewDto.getId());

        ApiResponse<ReviewDto> apiResponse = ApiResponse.<ReviewDto>builder()
                .data(updatedReviewDto)
                .message(String.format("The review with id %s was edited.", updatedReviewDto.getId()))
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}
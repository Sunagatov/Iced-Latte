package com.zufar.onlinestore.review.endpoint;

import com.zufar.onlinestore.review.converter.ReviewDtoConverter;
import com.zufar.onlinestore.review.dto.ReviewDto;
import com.zufar.onlinestore.review.entity.Review;
import com.zufar.onlinestore.review.exception.ReviewNotFoundException;
import com.zufar.onlinestore.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/products/reviews")
@Validated
@AllArgsConstructor
public class ReviewEndpoint {

    private final ReviewService reviewService;
    private final ReviewDtoConverter reviewDtoConverter;

    @PostMapping("/{productId}")
    @ResponseBody
    public ResponseEntity<String> addReview(@PathVariable("productId") String productId,
                                            @RequestBody @Valid ReviewDto request) {
        log.info("Received request to create Review - {}.", request);
        Review review = reviewDtoConverter.convertToEntity(request);
        review.setProductId(productId);
        reviewService.addReview(review);
        log.info("The Review was created");
        return ResponseEntity.status(HttpStatus.CREATED).body("Review is added");
    }

    @GetMapping("/{productId}/all")
    @ResponseBody
    public ResponseEntity<List<ReviewDto>> getProductReviews(@PathVariable("productId") String productId) {

        log.info("Received request to get all Reviews for Product");
        List<Review> reviews = reviewService.getProductReviews(productId);
        if (reviews.isEmpty()) {
            log.info("Reviews for product Id " + productId + " are absent.");
            return ResponseEntity.notFound().build();
        }
        List<ReviewDto> reviewDtos = reviewDtoConverter.convertToDtoList(reviews);
        log.info("All ProductInfos were retrieved - {}.", reviewDtos);
        return ResponseEntity.ok().body(reviewDtos);
    }


    @DeleteMapping("/{productId}/{reviewId}")
    @ResponseBody
    public ResponseEntity<String> deleteReview(@PathVariable("productId") String productId, @PathVariable("reviewId") String reviewId) {
        log.info("Received request to delete the Review with id - {}.", reviewId);

        try {
            reviewService.deleteReview(reviewId);
        } catch (ReviewNotFoundException exception) {
            log.info("Review with id " + reviewId + " not found");
            return ResponseEntity.ok("Review with id " + reviewId + " not found");
        }

        log.info("The Review with id - {} was deleted.", reviewId);
        return ResponseEntity.ok("Review successfully deleted");
    }

    @PutMapping("/{reviewId}")
    @ResponseBody
    public ResponseEntity<String> editReview(@PathVariable("reviewId") String reviewId,
                                             @RequestBody @Valid ReviewDto request) {
        log.info("Received request to edit the review with id - {}, request - {}.", reviewId, request);
        Optional<Review> optionalReview = reviewService.findReview(reviewId);
        if (optionalReview.isPresent()) {
            Review existingReview = optionalReview.get();
            existingReview.setText(request.getText());
            existingReview.setRating(request.getRating());
            reviewService.addReview(existingReview);
            log.info("The review with id - {} was edited.", reviewId);
            return ResponseEntity.ok("The review with id " + reviewId + " was  edited.");
        } else {
            log.info("The review with id - {} was not found.", reviewId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The review with id " + reviewId + " not found!");
        }
    }

}


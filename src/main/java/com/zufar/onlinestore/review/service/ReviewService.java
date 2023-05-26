package com.zufar.onlinestore.review.service;

import com.zufar.onlinestore.review.entity.Review;
import com.zufar.onlinestore.review.exception.ReviewNotFoundException;
import com.zufar.onlinestore.review.repository.ReviewRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public void addReview(Review review) {
        reviewRepository.save(review);
    }

    public Optional<Review> findReview(String reviewId) {
        return reviewRepository.findById(reviewId);
    }

    public List<Review> getProductReviews(String productId) {
        return reviewRepository.findByProductId(productId);
    }

    public void deleteReview(String reviewId) {

        Optional<Review> review = reviewRepository.findById(reviewId);
        if (review.isPresent()) {
            reviewRepository.deleteById(reviewId);
        } else {
            throw new ReviewNotFoundException("Review with id " + reviewId + " not found");
        }
    }
}


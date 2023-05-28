package com.zufar.onlinestore.review.service;

import com.zufar.onlinestore.review.dto.ReviewDto;
import com.zufar.onlinestore.review.entity.Review;
import com.zufar.onlinestore.review.exception.ReviewNotFoundException;
import com.zufar.onlinestore.review.repository.ReviewRepository;
import com.zufar.onlinestore.review.converter.ReviewDtoConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewDtoConverter reviewDtoConverter;

    @Transactional
    public ReviewDto addReview(ReviewDto reviewDto) {
        Review savedReview = reviewRepository.save(reviewDtoConverter.convertToEntity(reviewDto));
        return reviewDtoConverter.convertToDto(savedReview);
    }

    public Optional<ReviewDto> findReview(String reviewId) {
        Optional<Review> review = reviewRepository.findById(reviewId);
        return review.map(reviewDtoConverter::convertToDto);
    }

    public List<ReviewDto> getProductReviews(String productId) {
        List<Review> reviews = reviewRepository.findAllByProductId(productId);
        return reviewDtoConverter.convertToDtoList(reviews);
    }

    @Transactional
    public String deleteReview(String reviewId) {
        AtomicReference<String> deletedId = new AtomicReference<>();
        reviewRepository.findById(reviewId).ifPresentOrElse(
                review -> {
                    reviewRepository.deleteById(reviewId);
                    deletedId.set(reviewId);
                },
                () -> {
                    throw new ReviewNotFoundException(reviewId);
                }
        );
        return deletedId.get();
    }
}
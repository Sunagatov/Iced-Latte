package com.zufar.onlinestore.review.service;

import com.zufar.onlinestore.review.converter.ReviewDtoConverter;
import com.zufar.onlinestore.review.dto.ReviewDto;
import com.zufar.onlinestore.review.entity.Review;
import com.zufar.onlinestore.review.exception.ReviewDeleteFailedException;
import com.zufar.onlinestore.review.exception.ReviewNotFoundException;
import com.zufar.onlinestore.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
        return Optional.ofNullable(reviewRepository.findById(reviewId)
                .map(reviewDtoConverter::convertToDto)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId)));
    }

    public List<ReviewDto> getProductReviews(String productId) {
        return reviewDtoConverter.convertToDtoList(reviewRepository.findAllByProductId(productId));
    }

    @Transactional
    public String deleteReview(String reviewId) {
        reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException(reviewId));
        reviewRepository.deleteById(reviewId);

        if (reviewRepository.existsById(reviewId)) {
            throw new ReviewDeleteFailedException(reviewId);
        }

        return reviewId;
    }
}
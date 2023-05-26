package com.zufar.onlinestore.review.converter;

import com.zufar.onlinestore.review.dto.ReviewDto;
import com.zufar.onlinestore.review.entity.Review;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewDtoConverter {

    public ReviewDto convertToDto(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .text(review.getText())
                .rating(review.getRating())
                .productId(review.getProductId())
                .customerId(review.getCustomerId())
                .build();
    }

    public Review convertToEntity(ReviewDto reviewDto) {
        return Review.builder()
                .id(reviewDto.getId())
                .text(reviewDto.getText())
                .rating(reviewDto.getRating())
                .productId(reviewDto.getProductId())
                .customerId(reviewDto.getCustomerId())
                .build();
    }

    public List<ReviewDto> convertToDtoList(List<Review> entities) {
        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}

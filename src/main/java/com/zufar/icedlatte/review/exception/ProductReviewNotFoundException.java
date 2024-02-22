package com.zufar.icedlatte.review.exception;

import com.zufar.icedlatte.common.exception.ResourceNotFoundException;
import java.util.UUID;

public class ProductReviewNotFoundException extends ResourceNotFoundException {

    private final UUID reviewId;

    public ProductReviewNotFoundException(UUID reviewId) {
        super(String.format("Review entity with id %s doesn't exist", reviewId));
        this.reviewId = reviewId;
    }
}

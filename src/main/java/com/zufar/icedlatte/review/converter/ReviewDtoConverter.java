package com.zufar.icedlatte.review.converter;

import com.zufar.icedlatte.openapi.dto.ReviewResponse;
import com.zufar.icedlatte.review.entity.Review;
import org.springframework.stereotype.Service;

@Service
public class ReviewDtoConverter {

    public ReviewResponse toReviewResponse(Review review){
        var response = new ReviewResponse();
        response.setReviewId(review.getId().toString());
        response.setCreatedAt(review.getCreatedAt());
        response.setText(review.getText());
        return response;
    }
}

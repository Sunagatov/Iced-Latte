package com.zufar.icedlatte.review.converter;

import com.zufar.icedlatte.openapi.dto.ProductReviewResponse;
import com.zufar.icedlatte.openapi.dto.ProductReviewWithRating;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class ProductReviewDtoConverter {

    public ProductReviewResponse toReviewResponse(ProductReview productReview){
        var response = new ProductReviewResponse();
        response.setProductReviewId(productReview.getId());
        response.setCreatedAt(productReview.getCreatedAt());
        response.setText(productReview.getText());
        return response;
    }

    public ProductReviewsAndRatingsWithPagination toProductReviewsAndRatingsWithPagination(final Page<ProductReviewWithRating> productReviewWithRatingPage){
        var result = new ProductReviewsAndRatingsWithPagination();
        result.setPage(productReviewWithRatingPage.getTotalPages());
        result.setSize(productReviewWithRatingPage.getSize());
        result.setTotalElements(productReviewWithRatingPage.getTotalElements());
        result.setTotalPages(productReviewWithRatingPage.getTotalPages());
        result.setReviewsWithRatings(productReviewWithRatingPage.getContent());
        return result;
    }

}

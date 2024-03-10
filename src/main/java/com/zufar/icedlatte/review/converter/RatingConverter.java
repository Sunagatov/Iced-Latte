package com.zufar.icedlatte.review.converter;

import com.zufar.icedlatte.openapi.dto.ProductRatingDto;
import com.zufar.icedlatte.review.entity.Rating;
import com.zufar.icedlatte.review.exception.RatingConverterException;
import org.springframework.stereotype.Service;

@Service
public class RatingConverter {

    public ProductRatingDto convertToDto(Rating rating) {
        if (rating.getUser() == null) {
            throw new RatingConverterException("Can't convert rating without user");
        }

        if (rating.getProductInfo() == null) {
            throw new RatingConverterException("Can't convert rating without product");
        }

        if (rating.getProductRating() == null) {
            throw new RatingConverterException("Can't convert rating without product's rating");
        }

        return new ProductRatingDto(
                rating.getProductInfo().getProductId(),
                rating.getUser().getId(),
                rating.getProductRating()
        );
    }
}

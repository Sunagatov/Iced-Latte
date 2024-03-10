package com.zufar.icedlatte.review.converter;

import com.zufar.icedlatte.openapi.dto.ProductRatingDto;
import com.zufar.icedlatte.review.entity.ProductRating;
import com.zufar.icedlatte.review.exception.RatingConverterException;
import org.springframework.stereotype.Service;

@Service
public class RatingConverter {

    public ProductRatingDto convertToDto(ProductRating productRating) {
        if (productRating.getUser() == null) {
            throw new RatingConverterException("Can't convert rating without user");
        }

        if (productRating.getProductInfo() == null) {
            throw new RatingConverterException("Can't convert rating without product");
        }

        if (productRating.getProductRating() == null) {
            throw new RatingConverterException("Can't convert rating without product's rating");
        }

        return new ProductRatingDto(
                productRating.getProductInfo().getProductId(),
                productRating.getUser().getId(),
                productRating.getProductRating()
        );
    }
}

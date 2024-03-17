package com.zufar.icedlatte.review.converter;

import com.zufar.icedlatte.openapi.dto.ProductRatingDto;
import com.zufar.icedlatte.review.entity.ProductRating;
import org.springframework.stereotype.Service;

@Service
public class ProductRatingDtoConverter {

    public ProductRatingDto convertToDto(ProductRating productRating) {
        return new ProductRatingDto(
                productRating.getProductInfo().getProductId(),
                productRating.getProductRating()
        );
    }
}

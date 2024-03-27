package com.zufar.icedlatte.review.converter;

import com.zufar.icedlatte.openapi.dto.ProductRatingDto;
import com.zufar.icedlatte.openapi.dto.RatingMap;
import com.zufar.icedlatte.review.entity.ProductRating;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductRatingDtoConverter {

    public ProductRatingDto convertToDto(ProductRating productRating) {
        return new ProductRatingDto(
                productRating.getProductInfo().getProductId(),
                productRating.getProductRating()
        );
    }

    public RatingMap convertToRatingMap(List<Object[]> listOfMappings) {
        var map = new RatingMap();
        for (Object[] arr : listOfMappings) {
            var rating = (Integer) arr[0];
            var count = ((Long) arr[1]).intValue();
            switch (rating) {
                case 5:
                    map._5(count);
                    break;
                case 4:
                    map._4(count);
                    break;
                case 3:
                    map._3(count);
                    break;
                case 2:
                    map._2(count);
                    break;
                case 1:
                    map._1(count);
                    break;
                default:
                    assert false : "Unexpected rating value";
            }
        }
        return map;
    }
}

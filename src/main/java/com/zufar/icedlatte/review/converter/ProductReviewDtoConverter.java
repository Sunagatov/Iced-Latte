package com.zufar.icedlatte.review.converter;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.openapi.dto.RatingMap;
import com.zufar.icedlatte.review.dto.ProductRatingCount;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.FIELD)
public interface ProductReviewDtoConverter {

    @Mapping(target = "productReviewId", source = "id")
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "userLastname", ignore = true)
    ProductReviewDto toProductReviewDto(ProductReview productReview);

    default ProductReviewDto toProductReviewDto(ProductReview productReview, UserLookupSnapshot user) {
        ProductReviewDto dto = toProductReviewDto(productReview);
        if (user != null) {
            dto.setUserName(user.firstName());
            dto.setUserLastname(user.lastName());
        }
        return dto;
    }

    @Mapping(target = "page", expression = "java(page.getNumber())")
    @Mapping(target = "size", expression = "java(page.getSize())")
    @Mapping(target = "totalElements", expression = "java(page.getTotalElements())")
    @Mapping(target = "totalPages", expression = "java(page.getTotalPages())")
    @Mapping(target = "reviewsWithRatings", expression = "java(page.getContent())")
    ProductReviewsAndRatingsWithPagination toProductReviewsAndRatingsWithPagination(final Page<ProductReviewDto> page);

    default RatingMap convertToProductRatingMap(List<ProductRatingCount> productRatingCountPairs) {
        var productRatingMap = new RatingMap();
        for (ProductRatingCount productRatingCount : productRatingCountPairs) {
            int count = (int) productRatingCount.count();
            int rating = productRatingCount.productRating();
            switch (rating) {
                case 5 -> productRatingMap.setStar5(count);
                case 4 -> productRatingMap.setStar4(count);
                case 3 -> productRatingMap.setStar3(count);
                case 2 -> productRatingMap.setStar2(count);
                case 1 -> productRatingMap.setStar1(count);
                default -> throw new BadRequestException("Invalid product rating value.");
            }
        }
        return productRatingMap;
    }
}

package com.zufar.icedlatte.review.converter;

import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.openapi.dto.RatingMap;
import com.zufar.icedlatte.review.dto.ProductRatingCount;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE, injectionStrategy = InjectionStrategy.FIELD)
public interface ProductReviewDtoConverter {

   ProductReviewDto EMPTY_PRODUCT_REVIEW_RESPONSE =
            new ProductReviewDto(null, null, null, null, null, null, null, null, null);

    @Mapping(target = "productReviewId", source = "id")
    @Mapping(target = "userName", source = "user", qualifiedByName = "toUserName")
    @Mapping(target = "userLastname", source = "user", qualifiedByName = "toUserLastName")
    ProductReviewDto toProductReviewDto(ProductReview productReview);

    @Mapping(target = "page", expression = "java(page.getNumber())")
    @Mapping(target = "size", expression = "java(page.getSize())")
    @Mapping(target = "totalElements", expression = "java(page.getTotalElements())")
    @Mapping(target = "totalPages", expression = "java(page.getTotalPages())")
    @Mapping(target = "reviewsWithRatings", expression = "java(page.getContent())")
    ProductReviewsAndRatingsWithPagination toProductReviewsAndRatingsWithPagination(final Page<ProductReviewDto> page);

    @Named("toUserName")
    default String convertToUserName(UserEntity user) {
        return user == null ? null : user.getFirstName();
    }

    @Named("toUserLastName")
    default String convertToUserLastName(UserEntity user) {
        return user == null ? null : user.getLastName();
    }

    default RatingMap convertToProductRatingMap(List<ProductRatingCount> productRatingCountPairs) {
        var productRatingMap = new RatingMap();

        for (ProductRatingCount productRatingCount : productRatingCountPairs) {
            var productRating = productRatingCount.productRating();
            var count = (int) productRatingCount.count();

            switch (productRating) {
                case 5:
                    productRatingMap.setStar5(count);
                    break;
                case 4:
                    productRatingMap.setStar4(count);
                    break;
                case 3:
                    productRatingMap.setStar3(count);
                    break;
                case 2:
                    productRatingMap.setStar2(count);
                    break;
                case 1:
                    productRatingMap.setStar1(count);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected product's rating value: " + productRating);
            }
        }
        return productRatingMap;
    }
}

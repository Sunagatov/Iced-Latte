package com.zufar.icedlatte.favorite.converter;

import static java.util.Comparator.comparing;

import java.util.List;

import org.springframework.stereotype.Component;

import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.dto.ProductSummaryDto;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

@Component
public class FavoriteListDtoConverter {

    public ListOfFavoriteProductsDto toDto(final List<ProductSnapshot> productSnapshots) {
        List<ProductSummaryDto> products = productSnapshots.stream()
                .sorted(comparing(productSnapshot -> productSnapshot.id().toString()))
                .map(FavoriteListDtoConverter::toSummaryDto)
                .toList();

        return new ListOfFavoriteProductsDto(products);
    }

    private static ProductSummaryDto toSummaryDto(ProductSnapshot productInfo) {
        return new ProductSummaryDto()
                .id(productInfo.id())
                .name(productInfo.name())
                .description(productInfo.description())
                .price(productInfo.price())
                .productFileUrl(productInfo.productFileUrl())
                .averageRating(productInfo.averageRating())
                .reviewsCount(productInfo.reviewsCount())
                .brandName(productInfo.brandName())
                .sellerName(productInfo.sellerName())
                .weight(productInfo.weight());
    }
}

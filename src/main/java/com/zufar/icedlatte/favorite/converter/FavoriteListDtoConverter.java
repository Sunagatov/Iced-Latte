package com.zufar.icedlatte.favorite.converter;

import static java.util.Comparator.comparing;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.dto.ProductSummaryDto;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

@Component
public class FavoriteListDtoConverter {

    public ListOfFavoriteProductsDto toDto(
            final FavoriteListEntity entity, final Map<UUID, ProductSnapshot> productsById) {
        List<ProductSummaryDto> products = entity.getFavoriteItems().stream()
                .map(FavoriteItemEntity::getProductId)
                .sorted(comparing(UUID::toString))
                .filter(productsById::containsKey)
                .distinct()
                .map(productsById::get)
                .map(FavoriteListDtoConverter::toSummaryDto)
                .toList();

        return new ListOfFavoriteProductsDto(products);
    }

    private static ProductSummaryDto toSummaryDto(ProductSnapshot productInfo) {
        return new ProductSummaryDto()
                .id(productInfo.id())
                .name(productInfo.name())
                .price(productInfo.price())
                .productFileUrl(productInfo.productFileUrl());
    }
}

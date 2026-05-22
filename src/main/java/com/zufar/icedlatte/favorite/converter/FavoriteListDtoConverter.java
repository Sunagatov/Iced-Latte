package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.dto.ProductSummaryDto;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FavoriteListDtoConverter {

    private final ProductInfoDtoConverter productInfoDtoConverter;

    public ListOfFavoriteProductsDto toDto(final FavoriteListEntity entity, final Map<UUID, ProductSnapshot> productsById) {
        List<ProductSummaryDto> products = entity.getFavoriteItems().stream()
                .map(FavoriteItemEntity::getProductId)
                .filter(productsById::containsKey)
                .distinct()
                .map(productsById::get)
                .map(productInfoDtoConverter::toSummaryDto)
                .toList();

        return new ListOfFavoriteProductsDto(products);
    }
}

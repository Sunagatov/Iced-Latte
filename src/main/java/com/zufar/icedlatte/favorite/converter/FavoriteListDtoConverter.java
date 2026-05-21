package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class FavoriteListDtoConverter {

    public ListOfFavoriteProductsDto toDto(final FavoriteListEntity entity, final Map<UUID, ProductSnapshot> productsById) {
        List<ProductInfoDto> products = entity.getFavoriteItems().stream()
                .map(FavoriteItemEntity::getProductId)
                .filter(productsById::containsKey)
                .distinct()
                .map(productsById::get)
                .map(this::toProductInfoDto)
                .toList();

        return new ListOfFavoriteProductsDto(products);
    }

    private ProductInfoDto toProductInfoDto(ProductSnapshot product) {
        return new ProductInfoDto()
                .id(product.id())
                .name(product.name())
                .price(product.price())
                .productFileUrl(product.productFileUrl());
    }
}

package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Context;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteItemDtoConverter {

    private final ProductInfoDtoConverter productInfoDtoConverter;

    public FavoriteItemDto toDto(@Context final ProductInfoDtoConverter productInfoDtoConverter,
                          final FavoriteItemEntity favoriteItemEntity){
        return new FavoriteItemDto(
                favoriteItemEntity.getId(),
                productInfoDtoConverter.toDto(favoriteItemEntity.getProductInfo())
        );
    }

    public Set<FavoriteItemDto> toSetDto(final Set<FavoriteItemEntity> favoriteItemEntities){
        return favoriteItemEntities.stream()
                .map(entity -> toDto(productInfoDtoConverter, entity))
                .collect(Collectors.toSet());
    }
}
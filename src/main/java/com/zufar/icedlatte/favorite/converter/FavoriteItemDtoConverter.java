package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.mapstruct.Context;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FavoriteItemDtoConverter {
    private ProductInfoDtoConverter productInfoDtoConverter;
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
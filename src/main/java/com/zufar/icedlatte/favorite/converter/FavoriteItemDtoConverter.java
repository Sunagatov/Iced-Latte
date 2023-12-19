package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.mapstruct.*;

import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = ProductInfoDtoConverter.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,  injectionStrategy = InjectionStrategy.FIELD)
public interface FavoriteItemDtoConverter {

    @Mapping(target = "productInfo", source = "productInfo")
    FavoriteItemDto toDto(@Context final ProductInfoDtoConverter productInfoDtoConverter,
                          final FavoriteItemEntity favoriteItemEntity);

    @Named("toSetFavoriteItemDto")
    Set<FavoriteItemDto> toSetDto(final Set<FavoriteItemEntity> favoriteItemEntities);
}
package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.product.converter.ProductInfoDtoMapstructConverter;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.InjectionStrategy;

import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = ProductInfoDtoMapstructConverter.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,  injectionStrategy = InjectionStrategy.FIELD)
public interface FavoriteItemDtoConverter {

    @Mapping(target = "productInfo", source = "productInfo", qualifiedByName = "toProductInfoDto")
    FavoriteItemDto toDto(final FavoriteItemEntity favoriteItemEntity);

    @Named("toSetFavoriteItemDto")
    Set<FavoriteItemDto> toSetDto(final Set<FavoriteItemEntity> favoriteItemEntities);
}
package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteList;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = FavoriteItemDtoConverter.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,  injectionStrategy = InjectionStrategy.FIELD)
public interface FavoriteListDtoConverter {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "favoriteItems", source = "favoriteItems", qualifiedByName = "toSetFavoriteItemDto")
    FavoriteListDto toDto(final FavoriteList favoriteList);
}
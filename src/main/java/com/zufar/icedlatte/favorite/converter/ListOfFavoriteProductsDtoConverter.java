package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.openapi.dto.ListOfFavoriteProductsDto;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.MappingConstants;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE, injectionStrategy = InjectionStrategy.FIELD)
public interface ListOfFavoriteProductsDtoConverter {

    default ListOfFavoriteProductsDto toListProductDto(FavoriteListDto favoriteList) {
        ListOfFavoriteProductsDto listOfFavoriteProductsDto = new ListOfFavoriteProductsDto();
        for (FavoriteItemDto item : favoriteList.favoriteItems()) {
            listOfFavoriteProductsDto.addProductsItem(item.productInfo());
        }
        return listOfFavoriteProductsDto;
    }

    @Named("toListProductInfoDto")
    default List<ProductInfoDto> toProductInfoDto(final Set<FavoriteItemDto> favoriteItems) {
        return favoriteItems.stream()
                .map(FavoriteItemDto::productInfo)
                .toList();
    }
}
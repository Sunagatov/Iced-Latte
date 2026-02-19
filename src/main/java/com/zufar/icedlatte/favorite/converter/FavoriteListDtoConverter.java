package com.zufar.icedlatte.favorite.converter;

import com.zufar.icedlatte.favorite.dto.FavoriteItemDto;
import com.zufar.icedlatte.favorite.dto.FavoriteListDto;
import com.zufar.icedlatte.favorite.entity.FavoriteItemEntity;
import com.zufar.icedlatte.favorite.entity.FavoriteListEntity;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = FavoriteItemDtoConverter.class, unmappedTargetPolicy = ReportingPolicy.IGNORE, injectionStrategy = InjectionStrategy.FIELD)
public interface FavoriteListDtoConverter {
    @Mapping(target = "favoriteItems", source = "favoriteItems", qualifiedByName = "mapFavoriteItems")
    FavoriteListDto toDto(final FavoriteListEntity favoriteListEntity);

    @Mapping(target = "dateAdded", source = "dateAdded", qualifiedByName = "localToOffsetDate")
    ProductInfoDto convertProductInfoDto(ProductInfo productInfo);

    @Named("mapFavoriteItems")
    default FavoriteItemDto toFavoriteItemDto(FavoriteItemEntity itemEntity) {
        UUID favoriteItemEntityId = itemEntity.getId();
        ProductInfo productInfo = itemEntity.getProductInfo();

        ProductInfoDto productInfoDto = convertProductInfoDto(productInfo);

        return new FavoriteItemDto(favoriteItemEntityId, productInfoDto);
    }

    @Named("localToOffsetDate")
    default OffsetDateTime offsetToLocalDate(LocalDateTime value) {
        if (value != null) {
            return OffsetDateTime.of(value, ZoneOffset.UTC);
        }
        return null;
    }

}
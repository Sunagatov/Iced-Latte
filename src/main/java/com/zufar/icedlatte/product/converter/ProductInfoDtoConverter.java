package com.zufar.icedlatte.product.converter;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductInfoDtoConverter {

    @Named("toProductInfoDto")
    @Mapping(target = "id", source = "productId")
    @Mapping(target = "averageRating", source = "averageRating", qualifiedByName = "roundAverageRatingValue")
    @Mapping(target = "dateAdded", source = "dateAdded", qualifiedByName = "localToOffsetDate")
    ProductInfoDto toDto(ProductInfo entity);

    @Mapping(target = "products", source = "content")
    @Mapping(target = "page", source = "number")
    @Mapping(target = "size", source = "size")
    ProductListWithPaginationInfoDto toProductPaginationDto(Page<ProductInfoDto> pageProductResponseDto);

    @Named("roundAverageRatingValue")
    default BigDecimal roundAverageRatingValue(BigDecimal averageRating) {
        return (averageRating == null) ? null : averageRating.setScale(1, RoundingMode.HALF_UP);
    }

    @Named("localToOffsetDate")
    default OffsetDateTime localToOffsetDate(LocalDateTime value) {
        return (value == null) ? null : value.atOffset(ZoneOffset.UTC);
    }
}

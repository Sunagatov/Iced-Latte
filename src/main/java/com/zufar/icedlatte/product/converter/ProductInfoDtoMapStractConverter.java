package com.zufar.icedlatte.product.converter;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductInfoDtoMapStractConverter {

    @Named("toProductInfoDto")
    @Mapping(target = "id", source = "entity.productId")
    ProductInfoDto toDto(final ProductInfo entity);

    @Mapping(target = "products", source = "pageProductResponseDto.content")
    @Mapping(target = "page", source = "pageProductResponseDto.number")
    @Mapping(target = "size", source = "pageProductResponseDto.size")
    ProductListWithPaginationInfoDto toProductPaginationDto(final Page<ProductInfoDto> pageProductResponseDto);
}

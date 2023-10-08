package com.zufar.onlinestore.product.converter;

import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.openapi.payment.dto.ProductInfoFullDto;
import com.zufar.onlinestore.product.dto.ProductListWithPaginationInfoDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface ProductInfoDtoConverter {

    @Mapping(target = "id", source = "entity.productId")
    ProductInfoDto toDto(final ProductInfo entity);

    @Named("toProductInfoFullDto")
    @Mapping(target = "id", source = "entity.productId")
    ProductInfoFullDto toFullDto(final ProductInfo entity);

    @Mapping(target = "products", source = "pageProductResponseDto.content")
    @Mapping(target = "page", source = "pageProductResponseDto.number")
    @Mapping(target = "size", source = "pageProductResponseDto.size")
    ProductListWithPaginationInfoDto toProductPaginationDto(final Page<ProductInfoDto> pageProductResponseDto);
}

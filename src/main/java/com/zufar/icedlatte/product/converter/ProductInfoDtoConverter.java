package com.zufar.icedlatte.product.converter;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;

public interface ProductInfoDtoConverter {

    @Named("toProductInfoDto")
    ProductInfoDto toDto(final ProductInfo productInfo);

    ProductListWithPaginationInfoDto toProductPaginationDto(final Page<ProductInfoDto> pageProductResponseDto);
}

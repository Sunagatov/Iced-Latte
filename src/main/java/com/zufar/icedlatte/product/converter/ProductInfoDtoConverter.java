package com.zufar.icedlatte.product.converter;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductInfoDtoConverter {

    public ProductInfoDto toDto(final ProductInfo entity){
        return new ProductInfoDto(
           entity.getProductId(),
           entity.getName(),
           entity.getPrice(),
           entity.getQuantity(),
           entity.getActive()
        );
    }

    ProductInfo toProductInfo(final ProductInfoDto dto);

    public ProductListWithPaginationInfoDto toProductPaginationDto(final Page<ProductInfoDto> pageProductResponseDto){
        List<ProductInfoDto> productInfoDtoList = pageProductResponseDto.getContent();

        return new ProductListWithPaginationInfoDto(
                productInfoDtoList,
                pageProductResponseDto.getNumber(),
                pageProductResponseDto.getSize(),
                pageProductResponseDto.getTotalElements(),
                pageProductResponseDto.getTotalPages()
        );
    }
}

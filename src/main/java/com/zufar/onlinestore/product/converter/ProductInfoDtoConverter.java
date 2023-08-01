package com.zufar.onlinestore.product.converter;

import com.zufar.onlinestore.product.dto.PriceDetailsDto;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.dto.ProductInfoFullDto;
import com.zufar.onlinestore.product.dto.ProductListWithPaginationInfoDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ProductInfoDtoConverter {

    public ProductInfoDto toDto(final ProductInfo entity) {
        PriceDetailsDto priceDetails = new PriceDetailsDto(entity.getPrice(), entity.getCurrency());

        return new ProductInfoDto(
                entity.getId(),
                entity.getDescription(),
                entity.getName(),
                priceDetails,
                entity.getQuantity()
        );
    }

    public ProductInfoFullDto toFullDto(final ProductInfo entity) {
        PriceDetailsDto priceDetails = new PriceDetailsDto(entity.getPrice(), entity.getCurrency());

        return new ProductInfoFullDto(
                entity.getId(),
                entity.getDescription(),
                entity.getName(),
                priceDetails,
                entity.getQuantity(),
                entity.getActive()
        );
    }

    public ProductListWithPaginationInfoDto toProductPaginationDto(final Page<ProductInfoDto> pageProductResponseDto) {
        return new ProductListWithPaginationInfoDto(
                pageProductResponseDto.getContent(),
                pageProductResponseDto.getNumber(),
                pageProductResponseDto.getSize(),
                pageProductResponseDto.getTotalElements(),
                pageProductResponseDto.getTotalPages()
        );
    }
}
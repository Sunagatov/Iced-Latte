package com.zufar.onlinestore.product.mapper;

import com.zufar.onlinestore.product.dto.PriceDetailsDto;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.dto.ProductInfoFullDto;
import com.zufar.onlinestore.product.dto.ProductPaginationDto;
import com.zufar.onlinestore.product.dto.ProductResponseDto;
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
                priceDetails
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

    public ProductResponseDto toResponseDto(final ProductInfo entity) {
        PriceDetailsDto priceDetails = new PriceDetailsDto(entity.getPrice(), entity.getCurrency());

        return new ProductResponseDto(
                entity.getId(),
                entity.getDescription(),
                entity.getName(),
                priceDetails,
                entity.getQuantity()
        );
    }

    public ProductInfo toEntity(final ProductInfoFullDto dto) {
        return new ProductInfo(
                dto.id(),
                dto.name(),
                dto.description(),
                dto.priceDetails().price(),
                dto.priceDetails().currency(),
                dto.quantity(),
                dto.active()
        );
    }

    public ProductPaginationDto addToProductPaginationDto(Page<ProductResponseDto> pageProductResponseDto) {
        return new ProductPaginationDto(
                pageProductResponseDto.getContent(),
                pageProductResponseDto.getNumber(),
                pageProductResponseDto.getSize(),
                pageProductResponseDto.getTotalElements(),
                pageProductResponseDto.getTotalPages()
        );
    }
}
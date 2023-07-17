package com.zufar.onlinestore.product.converter;

import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import org.springframework.stereotype.Component;

@Component
public class ProductInfoDtoConverter {

    public ProductInfoDto convertToDto(final ProductInfo entity) {
        return ProductInfoDto.builder()
                .id(entity.getId())
                .description(entity.getDescription())
                .name(entity.getName())
                .price(entity.getPrice())
                .currency(entity.getCurrency())
                .build();
    }

    public ProductInfo convertToEntity(final ProductInfoDto dto) {
        return ProductInfo.builder()
                .description(dto.description())
                .name(dto.name())
                .price(dto.price())
                .currency(dto.currency())
                .build();
    }
}
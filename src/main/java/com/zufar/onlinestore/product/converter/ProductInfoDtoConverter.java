package com.zufar.onlinestore.product.converter;

import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class ProductInfoDtoConverter {

    public ProductInfoDto convertToDto(final ProductInfo entity) {
        return ProductInfoDto.builder()
                .id(entity.getId())
                .category(entity.getCategory())
                .name(entity.getName())
                .price(entity.getPrice())
                .build();
    }

    public ProductInfo convertToEntity(final ProductInfoDto dto) {
        return ProductInfo.builder()
                .category(dto.getCategory())
                .name(dto.getName())
                .price(dto.getPrice())
                .build();
    }
}
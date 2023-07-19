package com.zufar.onlinestore.product.converter;

import com.zufar.onlinestore.product.dto.PriceDetailsDto;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import org.springframework.stereotype.Component;

@Component
public class ProductInfoDtoConverter {

    public ProductInfoDto convertToDto(final ProductInfo entity) {
        PriceDetailsDto priceDetailsDto = new PriceDetailsDto(entity.getPrice(), entity.getCurrency());

        return new ProductInfoDto(
                entity.getId(),
                entity.getDescription(),
                entity.getName(),
                priceDetailsDto
        );
    }
}
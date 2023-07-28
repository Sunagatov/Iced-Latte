package com.zufar.onlinestore.product.converter;

import com.zufar.onlinestore.product.dto.PriceDetailsDto;
import com.zufar.onlinestore.product.dto.ProductInfoDto;
import com.zufar.onlinestore.product.dto.ProductInfoFullDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import org.springframework.stereotype.Component;

@Component
public class ProductInfoDtoConverter {

    public ProductInfoDto convertToDto(final ProductInfo entity) {
        PriceDetailsDto priceDetails = new PriceDetailsDto(entity.getPrice(), entity.getCurrency());

        return new ProductInfoDto(
                entity.getId(),
                entity.getDescription(),
                entity.getName(),
                priceDetails
        );
    }

    public ProductInfoFullDto convertToFullDto(final ProductInfo entity) {
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

    public ProductInfo convertToEntity(final ProductInfoFullDto dto) {
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
}
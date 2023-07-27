package com.zufar.onlinestore.cart.converter;

import com.zufar.onlinestore.cart.dto.ShoppingSessionItemDto;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.product.mapper.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.dto.ProductInfoFullDto;
import com.zufar.onlinestore.product.entity.ProductInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShoppingSessionItemDtoConverter {

    private final ProductInfoDtoConverter productInfoDtoConverter;

    public ShoppingSessionItemDto toDto(final ShoppingSessionItem entity) {
        ProductInfoFullDto productInfo = productInfoDtoConverter.toFullDto(entity.getProductInfo());

        return new ShoppingSessionItemDto(
                entity.getId(),
                entity.getShoppingSession(),
                productInfo,
                entity.getProductsQuantity()
        );
    }

    public ShoppingSessionItem toEntity(final ShoppingSessionItemDto dto) {
        ProductInfo productInfo = productInfoDtoConverter.toEntity(dto.productInfo());

        return new ShoppingSessionItem(
                dto.id(),
                dto.shoppingSession(),
                productInfo,
                dto.productsQuantity()
        );
    }
}
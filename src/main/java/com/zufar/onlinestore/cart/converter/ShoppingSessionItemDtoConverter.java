package com.zufar.onlinestore.cart.converter;

import com.zufar.onlinestore.cart.dto.ShoppingSessionItemDto;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.product.converter.ProductInfoDtoConverter;
import com.zufar.onlinestore.product.dto.ProductInfoFullDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShoppingSessionItemDtoConverter {

    private final ProductInfoDtoConverter productInfoDtoConverter;

    public ShoppingSessionItemDto toDto(final ShoppingSessionItem entity) {
        ProductInfoFullDto productInfo = productInfoDtoConverter.convertToFullDto(entity.getProductInfo());

        return new ShoppingSessionItemDto(
                entity.getId(),
                productInfo,
                entity.getProductsQuantity()
        );
    }
}
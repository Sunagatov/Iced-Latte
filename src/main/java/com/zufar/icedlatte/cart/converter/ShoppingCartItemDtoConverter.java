package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShoppingCartItemDtoConverter {

    private final ProductInfoDtoConverter productInfoDtoConverter;

    public ShoppingCartItemDto toDto(final ShoppingCartItem entity){
        return new ShoppingCartItemDto(
                entity.getId(),
                productInfoDtoConverter.toDto(entity.getProductInfo()),
               entity.getProductQuantity()
        );
    }
}

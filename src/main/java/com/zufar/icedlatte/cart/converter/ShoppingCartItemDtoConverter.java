package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;


@Service
public class ShoppingCartItemDtoConverter {
    private ProductInfoDtoConverter productInfoDtoConverter;

    public ShoppingCartItemDto toDto(final ShoppingCartItem entity){
        return new ShoppingCartItemDto(
                entity.getId(),
                productInfoDtoConverter.toDto(entity.getProductInfo()),
               entity.getProductQuantity()
        );
    }
}

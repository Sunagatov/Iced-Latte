package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.openapi.dto.ShoppingSessionItemDto;
import com.zufar.icedlatte.cart.entity.ShoppingSessionItem;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = ProductInfoDtoConverter.class)
public interface ShoppingSessionItemDtoConverter {

    @Named("toShoppingSessionItemDto")
    @Mapping(target = "productInfo", source = "entity.productInfo", qualifiedByName = {"toProductInfoDto"})
    ShoppingSessionItemDto toDto(final ShoppingSessionItem entity);
}

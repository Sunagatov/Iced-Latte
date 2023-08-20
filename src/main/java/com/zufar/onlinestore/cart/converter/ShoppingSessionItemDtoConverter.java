package com.zufar.onlinestore.cart.converter;

import com.zufar.onlinestore.cart.dto.ShoppingSessionItemDto;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.product.converter.ProductInfoDtoConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = ProductInfoDtoConverter.class)
public interface ShoppingSessionItemDtoConverter {

    @Named("toShoppingSessionItemDto")
    @Mapping(target = "productInfo", source = "entity.productInfo", qualifiedByName = {"toProductInfoFullDto"})
    ShoppingSessionItemDto toDto(final ShoppingSessionItem entity);
}
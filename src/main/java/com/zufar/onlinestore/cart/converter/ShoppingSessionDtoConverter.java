package com.zufar.onlinestore.cart.converter;

import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring", uses = ShoppingSessionItemDtoConverter.class)
public interface ShoppingSessionDtoConverter {

    @Mapping(target = "items", source = "entity.items", qualifiedByName = {"toShoppingSessionItemDto"})
    ShoppingSessionDto toDto(final ShoppingSession entity);
}
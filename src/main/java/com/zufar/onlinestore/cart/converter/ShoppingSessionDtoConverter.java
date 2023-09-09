package com.zufar.onlinestore.cart.converter;

import com.zufar.onlinestore.cart.api.ItemsTotalPriceCalculator;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ShoppingSessionItemDtoConverter.class, ItemsTotalPriceCalculator.class})
public interface ShoppingSessionDtoConverter {

    @Mapping(target = "items", source = "entity.items", qualifiedByName = {"toShoppingSessionItemDto"})
    @Mapping(target = "itemsTotalPrice", source = "entity.items", qualifiedByName = {"toItemsTotalPrice"})
    ShoppingSessionDto toDto(final ShoppingSession entity);
}

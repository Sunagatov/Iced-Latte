package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.api.ItemsTotalPriceCalculator;
import com.zufar.icedlatte.openapi.dto.ShoppingSessionDto;
import com.zufar.icedlatte.cart.entity.ShoppingSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ShoppingSessionItemDtoConverter.class, ItemsTotalPriceCalculator.class})
public interface ShoppingSessionDtoConverter {

    @Mapping(target = "items", source = "entity.items", qualifiedByName = {"toShoppingSessionItemDto"})
    @Mapping(target = "itemsTotalPrice", source = "entity.items", qualifiedByName = {"toItemsTotalPrice"})
    ShoppingSessionDto toDto(final ShoppingSession entity);
}

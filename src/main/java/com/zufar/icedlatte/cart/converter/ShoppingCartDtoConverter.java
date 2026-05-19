package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.api.ItemsTotalPriceCalculator;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {ShoppingCartItemDtoConverter.class, ItemsTotalPriceCalculator.class})
public interface ShoppingCartDtoConverter {

    @Mapping(target = "items", source = "items", qualifiedByName = {"toShoppingCartItemDto"})
    @Mapping(target = "itemsTotalPrice", source = "items", qualifiedByName = {"toItemsTotalPrice"})
    @Mapping(target = "itemsQuantity", expression = "java(cart.getItems() != null ? cart.getItems().size() : 0)")
    @Mapping(target = "productsQuantity", expression = "java(cart.getItems() != null ? cart.getItems().stream().mapToInt(com.zufar.icedlatte.cart.entity.ShoppingCartItem::getProductQuantity).sum() : 0)")
    ShoppingCartDto toDto(final ShoppingCart cart);
}

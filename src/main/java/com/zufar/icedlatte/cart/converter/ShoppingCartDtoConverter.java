package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.api.ItemsTotalPriceCalculator;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import java.math.BigDecimal;

import static com.zufar.icedlatte.cart.api.ItemsTotalPriceCalculator.DEFAULT_SHIPPING_COST;
import static com.zufar.icedlatte.cart.api.ItemsTotalPriceCalculator.DEFAULT_TAX_RATE;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ShoppingCartItemDtoConverter.class, ItemsTotalPriceCalculator.class})
public interface ShoppingCartDtoConverter {

    @Mapping(target = "items", source = "entity.items", qualifiedByName = {"toShoppingCartItemDto"})
    @Mapping(target = "itemsTotalPrice", source = "entity.items", qualifiedByName = {"toItemsTotalPrice"})
    @Mapping(target = "taxRate", expression = "java(getTaxRate())")
    @Mapping(target = "shippingCost", expression = "java(getShippingCost())")
    ShoppingCartDto toDto(final ShoppingCart entity);

    default BigDecimal getTaxRate() { return DEFAULT_TAX_RATE; }
    default BigDecimal getShippingCost() { return DEFAULT_SHIPPING_COST; }
}

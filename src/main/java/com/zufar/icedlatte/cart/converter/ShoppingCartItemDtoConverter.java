package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.product.converter.ProductInfoDtoMapStractConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = ProductInfoDtoMapStractConverter.class)
public interface ShoppingCartItemDtoConverter {

    @Named("toShoppingCartItemDto")
    @Mapping(target = "productInfo", source = "entity.productInfo", qualifiedByName = {"toProductInfoDto"})
    ShoppingCartItemDto toDto(final ShoppingCartItem entity);
}

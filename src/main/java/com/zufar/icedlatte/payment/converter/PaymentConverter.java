package com.zufar.icedlatte.payment.converter;

import com.zufar.icedlatte.cart.converter.ShoppingCartItemDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ProcessedPaymentDetailsDto;
import com.zufar.icedlatte.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.Set;

@Mapper(uses = ShoppingCartItemDtoConverter.class , componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentConverter {

    @Mapping(target = "items", source = "shoppingCartItems", qualifiedByName = {"toShoppingCartItemDto"})
    ProcessedPaymentDetailsDto toDto(final Payment payment,
                                     final Set<ShoppingCartItem> shoppingCartItems);

}

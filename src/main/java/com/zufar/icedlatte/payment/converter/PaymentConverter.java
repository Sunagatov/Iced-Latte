package com.zufar.icedlatte.payment.converter;

import com.zufar.icedlatte.cart.converter.ShoppingSessionItemDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingSessionItem;
import com.zufar.icedlatte.openapi.dto.ProcessedPaymentDetailsDto;
import com.zufar.icedlatte.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.Set;

@Mapper(uses = ShoppingSessionItemDtoConverter.class , componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentConverter {

    @Mapping(target = "items", source = "shoppingSessionItems", qualifiedByName = {"toShoppingSessionItemDto"})
    ProcessedPaymentDetailsDto toDto(final Payment payment,
                                     final Set<ShoppingSessionItem> shoppingSessionItems);

}

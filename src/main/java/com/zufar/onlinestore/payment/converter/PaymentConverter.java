package com.zufar.onlinestore.payment.converter;

import com.zufar.onlinestore.cart.converter.ShoppingSessionItemDtoConverter;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.Set;

@Mapper(uses = ShoppingSessionItemDtoConverter.class , componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentConverter {

    @Mapping(target = "items", source = "shoppingSessionItems", qualifiedByName = {"toShoppingSessionItemDto"})
    ProcessedPaymentDetailsDto toDto(final Payment payment, final Set<ShoppingSessionItem> shoppingSessionItems);

}

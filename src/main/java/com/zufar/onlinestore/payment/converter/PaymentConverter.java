package com.zufar.onlinestore.payment.converter;

import com.zufar.onlinestore.cart.converter.ShoppingSessionItemDtoConverter;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Mapper(componentModel = "spring")
public abstract class PaymentConverter {

    private final ShoppingSessionItemRepository shoppingSessionItemRepository;
    private final ShoppingSessionItemDtoConverter shoppingSessionItemDtoConverter;

    @Mapping(target = "items", expression = "java(shoppingSessionItemRepository.findAll().stream().map(shoppingSessionItemDtoConverter::toDto).toList())")
    public abstract ProcessedPaymentDetailsDto toDto(final Payment payment);

}

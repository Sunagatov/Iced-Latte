package com.zufar.icedlatte.payment.converter;

import com.zufar.icedlatte.cart.converter.ShoppingCartItemDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ProcessedPaymentDetailsDto;
import com.zufar.icedlatte.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.util.Set;

import static com.zufar.icedlatte.payment.calculator.PaymentPriceCalculator.DEFAULT_SHIPPING_COST;
import static com.zufar.icedlatte.payment.calculator.PaymentPriceCalculator.DEFAULT_TAX_RATE;

@Mapper(uses = ShoppingCartItemDtoConverter.class , componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentConverter {

    @Mapping(target = "items", source = "shoppingCartItems", qualifiedByName = {"toShoppingCartItemDto"})
    @Mapping(target = "taxRate", expression = "java(getTaxRate())")
    @Mapping(target = "shippingCost", expression = "java(getShippingCost())")
    ProcessedPaymentDetailsDto toDto(final Payment payment,
                                     final Set<ShoppingCartItem> shoppingCartItems);
    default BigDecimal getTaxRate() { return DEFAULT_TAX_RATE; }
    default BigDecimal getShippingCost() { return DEFAULT_SHIPPING_COST; }
}

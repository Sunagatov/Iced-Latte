package com.zufar.icedlatte.payment.converter;

import com.zufar.icedlatte.cart.converter.ShoppingCartItemDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ProcessedPaymentDetailsDto;
import com.zufar.icedlatte.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class PaymentConverter {

    public ProcessedPaymentDetailsDto toDto(final Payment payment,
                                     final Set<ShoppingCartItem> shoppingCartItems){
        return new ProcessedPaymentDetailsDto(

        );
    }

}

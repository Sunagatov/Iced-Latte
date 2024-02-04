package com.zufar.icedlatte.payment.converter;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ProcessedPaymentDetailsDto;
import com.zufar.icedlatte.payment.entity.Payment;
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

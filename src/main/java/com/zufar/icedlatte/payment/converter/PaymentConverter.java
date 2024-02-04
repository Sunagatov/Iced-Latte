package com.zufar.icedlatte.payment.converter;

import com.zufar.icedlatte.cart.converter.ShoppingCartItemDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.PaymentStatus;
import com.zufar.icedlatte.openapi.dto.ProcessedPaymentDetailsDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentConverter {

    private final ShoppingCartItemDtoConverter shoppingCartItemDtoConverter;

    public ProcessedPaymentDetailsDto toDto(final Payment payment,
                                     final Set<ShoppingCartItem> shoppingCartItems) {
        if ( payment == null && shoppingCartItems == null ) {
            return null;
        }

        ProcessedPaymentDetailsDto processedPaymentDetailsDto = new ProcessedPaymentDetailsDto();

        if ( payment != null ) {
            processedPaymentDetailsDto.setPaymentId( payment.getPaymentId() );
            processedPaymentDetailsDto.setItemsTotalPrice( payment.getItemsTotalPrice() );
            processedPaymentDetailsDto.setPaymentIntentId( payment.getPaymentIntentId() );
            processedPaymentDetailsDto.setStatus( paymentStatusToPaymentStatus( payment.getStatus() ) );
            processedPaymentDetailsDto.setDescription( payment.getDescription() );
        }
        processedPaymentDetailsDto.setItems( shoppingCartItemSetToShoppingCartItemDtoSet( shoppingCartItems ) );

        return processedPaymentDetailsDto;
    }

    protected Set<ShoppingCartItemDto> shoppingCartItemSetToShoppingCartItemDtoSet(Set<ShoppingCartItem> set) {
        if ( set == null ) {
            return null;
        }

        Set<ShoppingCartItemDto> set1 = new LinkedHashSet<ShoppingCartItemDto>( Math.max( (int) ( set.size() / .75f ) + 1, 16 ) );
        for ( ShoppingCartItem shoppingCartItem : set ) {
            set1.add( shoppingCartItemDtoConverter.toDto( shoppingCartItem ) );
        }

        return set1;
    }

    protected PaymentStatus paymentStatusToPaymentStatus(com.zufar.icedlatte.payment.enums.PaymentStatus paymentStatus) {
        if ( paymentStatus == null ) {
            return null;
        }

        PaymentStatus paymentStatus1 = new PaymentStatus();

        if ( paymentStatus.getStatus() != null ) {
            paymentStatus1.setStatus( Enum.valueOf( PaymentStatus.StatusEnum.class, paymentStatus.getStatus() ) );
        }
        if ( paymentStatus.getDescription() != null ) {
            paymentStatus1.setDescription( Enum.valueOf( PaymentStatus.DescriptionEnum.class, paymentStatus.getDescription() ) );
        }

        return paymentStatus1;
    }
}
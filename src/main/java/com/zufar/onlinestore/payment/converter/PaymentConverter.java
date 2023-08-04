package com.zufar.onlinestore.payment.converter;

import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentConverter {

    public PaymentDetailsDto toDto(final Payment entity) {
        return PaymentDetailsDto.builder()
                .paymentId(entity.getPaymentId())
                .paymentIntentId(entity.getPaymentIntentId())
                .totalPrice(entity.getItemsTotalPrice())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .build();
    }

    public Payment toEntity(final PaymentDetailsDto dto) {
        return Payment.builder()
                .paymentId(dto.paymentId())
                .paymentIntentId(dto.paymentIntentId())
                .itemsTotalPrice(dto.totalPrice())
                .currency(dto.currency())
                .description(dto.description())
                .status(dto.status())
                .build();
    }

}

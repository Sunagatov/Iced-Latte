package com.zufar.onlinestore.payment.mapper;

import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.model.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentConverter {

    public PaymentDetailsDto toDto(Payment entity) {
        return PaymentDetailsDto.builder()
                .paymentId(entity.getPaymentId())
                .totalPrice(entity.getItemsTotalPrice())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .build();
    }

    public Payment toEntity(PaymentDetailsDto dto) {
        return Payment.builder()
                .paymentId(dto.paymentId())
                .itemsTotalPrice(dto.totalPrice())
                .currency(dto.currency())
                .description(dto.description())
                .status(dto.status())
                .build();
    }
}

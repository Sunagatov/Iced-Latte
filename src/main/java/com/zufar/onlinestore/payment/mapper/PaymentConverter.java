package com.zufar.onlinestore.payment.mapper;

import com.zufar.onlinestore.payment.dto.PaymentResponseDto;
import com.zufar.onlinestore.payment.model.Payment;
import org.springframework.stereotype.Component;


@Component
public class PaymentConverter {

    public PaymentResponseDto toDto(Payment entity) {
        return PaymentResponseDto.builder()
                .paymentId(entity.getPaymentId())
                .totalPrice(entity.getTotalPrice())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .build();
    }

    public Payment toEntity(PaymentResponseDto dto) {
        return Payment.builder()
                .paymentId(dto.paymentId())
                .totalPrice(dto.totalPrice())
                .currency(dto.currency())
                .description(dto.description())
                .status(dto.status())
                .build();
    }

}

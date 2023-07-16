package com.zufar.onlinestore.payment.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentResponseDto(String paymentId,
                                 BigDecimal totalPrice,
                                 String currency,
                                 String status,
                                 String description) {
}

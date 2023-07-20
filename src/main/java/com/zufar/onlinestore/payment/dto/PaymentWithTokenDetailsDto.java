package com.zufar.onlinestore.payment.dto;

import lombok.Builder;

@Builder
public record PaymentWithTokenDetailsDto(String paymentToken, PaymentDetailsDto paymentDetailsDto) {}

package com.zufar.onlinestore.payment.dto;

import lombok.Builder;

@Builder
public record PaymentDetailsDto(PaymentResponseDto paymentResponseDto, String paymentToken){}

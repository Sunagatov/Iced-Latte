package com.zufar.onlinestore.payment.dto;

import lombok.Builder;

@Builder
public record PaymentDetailsWithTokenDto(String paymentToken,
                                         PaymentDetailsDto paymentDetailsDto) {
}

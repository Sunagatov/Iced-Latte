package com.zufar.onlinestore.payment.api.dto;

import lombok.Builder;

@Builder
public record ProcessedPaymentWithClientSecretDto(
        Long paymentId,
        String clientSecret
) {
}

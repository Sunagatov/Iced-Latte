package com.zufar.onlinestore.payment.api.dto;

import lombok.Builder;

@Builder
public record ProcessedPaymentWithClientSecretDto(
        String clientSecret,
        Long paymentId
) {
}

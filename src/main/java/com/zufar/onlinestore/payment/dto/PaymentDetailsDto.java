package com.zufar.onlinestore.payment.dto;

import com.zufar.onlinestore.payment.enums.PaymentStatus;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record PaymentDetailsDto(
        Long paymentId,
        BigDecimal itemsTotalPrice,
        String paymentIntentId,
        String currency,
        PaymentStatus status,
        String description
) {
}

package com.zufar.onlinestore.payment.dto;

import com.zufar.onlinestore.payment.enums.PaymentConstants;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record PaymentDetailsDto(
        Long paymentId,
        String paymentIntentId,
        BigDecimal itemsTotalPrice,
        PaymentConstants status,
        String description
) {
}

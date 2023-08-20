package com.zufar.onlinestore.payment.api.dto;

import com.zufar.onlinestore.cart.dto.ShoppingSessionItemDto;
import com.zufar.onlinestore.payment.enums.PaymentStatus;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.Set;

@Builder
public record ProcessedPaymentDetailsDto(
        Long paymentId,
        BigDecimal itemsTotalPrice,
        String paymentIntentId,
        Set<ShoppingSessionItemDto> items,
        PaymentStatus status,
        String description
) {
}

package com.zufar.onlinestore.payment.api.dto;

import lombok.Builder;

@Builder
public record CreateCardDetailsTokenDto(
        String cardNumber,
        String expMonth,
        String expYear,
        String cvc
) {
}

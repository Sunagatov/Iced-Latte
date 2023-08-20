package com.zufar.onlinestore.payment.api.dto;

import lombok.Builder;

@Deprecated
@Builder
public record CreateCardDetailsTokenDto(
        String cardNumber,
        String expMonth,
        String expYear,
        String cvc
) {
}

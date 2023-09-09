package com.zufar.onlinestore.payment.api.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record CreateCardDetailsTokenRequest(

        @NotEmpty(message = "CardNumber is the mandatory attribute")
        String cardNumber,

        @NotEmpty(message = "ExpMonth is the mandatory attribute")
        String expMonth,

        @NotEmpty(message = "ExpYear is the mandatory attribute")
        String expYear,

        @NotEmpty(message = "Cvc is the mandatory attribute")
        String cvc
) {
}

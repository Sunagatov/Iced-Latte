package com.zufar.onlinestore.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentMethodDto (

        @NotBlank(message = "CardNumber is the mandatory attribute")
        String cardNumber,

        @NotNull(message = "ExpMonth is the mandatory attribute")
        Long expMonth,

        @NotNull(message = "ExpYear is the mandatory attribute")
        Long expYear,

        @NotNull(message = "Cvc is the mandatory attribute")
        String cvc
) {
}
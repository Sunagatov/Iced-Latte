package com.zufar.onlinestore.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentMethodDto (

        @NotBlank(message = "cardNumber is the mandatory attribute")
        String cardNumber,

        @NotBlank(message = "expMonth is the mandatory attribute")
        Long expMonth,

        @NotBlank(message = "expYear is the mandatory attribute")
        Long expYear,

        @NotNull(message = "cvc is the mandatory attribute")
        String cvc) {}
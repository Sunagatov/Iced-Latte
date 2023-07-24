package com.zufar.onlinestore.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentDto(

        @NotBlank(message = "paymentMethodId is the mandatory attribute")
        String paymentMethodId,

        @NotNull(message = "PriceDetails is the mandatory attribute")
        PriceDetailsDto priceDetails) {
}

package com.zufar.onlinestore.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentDto(
        @NotBlank(message = "Payment method id is mandatory attribute")
        String paymentMethodId,
        @NotNull(message = "Price details is mandatory attribute")
        PriceDetailsDto priceDetails) {
}

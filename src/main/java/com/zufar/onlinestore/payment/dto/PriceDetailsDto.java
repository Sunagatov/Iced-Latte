package com.zufar.onlinestore.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PriceDetailsDto(
        @NotNull(message = "Total price is mandatory attribute")
        BigDecimal totalPrice,
        @NotBlank(message = "Currency is mandatory attribute")
        @Size(min = 3, max = 3, message = "Currency value must be only 3 characters long")
        String currency) {
}
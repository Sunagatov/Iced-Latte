package com.zufar.onlinestore.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PriceDetailsDto(
        @NotNull(message = "Price is mandatory")
        @DecimalMin(value = "0.0", message = "Price minimum value should be more than 0.0")
        BigDecimal price,

        @NotBlank(message = "Currency is mandatory")
        @Size(max = 55, message = "Currency length must be less than 55 characters")
        String currency
) {
}

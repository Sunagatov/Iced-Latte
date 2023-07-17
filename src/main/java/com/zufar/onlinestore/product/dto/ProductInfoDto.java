package com.zufar.onlinestore.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record ProductInfoDto(UUID id,

                             @NotBlank(message = "Name is mandatory")
                             @Size(max = 55, message = "Name length must be less than 55 characters")
                             String name,

                             @NotBlank(message = "Description is mandatory")
                             @Size(max = 55, message = "Description length must be less than 55 characters")
                             String description,

                             @NotNull(message = "Price is mandatory")
                             @DecimalMin(value = "10.0", message = "Price minimum value should be more than 10")
                             BigDecimal price,

                             @NotBlank(message = "Currency is mandatory")
                             @Size(max = 55, message = "Currency length must be less than 55 characters")
                             String currency) {
}

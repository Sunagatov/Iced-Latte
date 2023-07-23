package com.zufar.onlinestore.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductInfoFullDto(
        @NotNull(message = "Id is mandatory")
        UUID id,

        @NotBlank(message = "Name is mandatory")
        @Size(max = 100, message = "Name length must be less than 100 characters")
        String name,

        @NotBlank(message = "Description is mandatory")
        @Size(max = 1000, message = "Description length must be less than 1000 characters")
        String description,

        @NotNull(message = "PriceDetails is mandatory")
        PriceDetailsDto priceDetails,

        @NotNull(message = "Quantity  is mandatory")
        Integer quantity,

        @NotNull(message = "Active is mandatory")
        Boolean active
) {
}

package com.zufar.onlinestore.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductInfoFullDto(

        @NotNull(message = "Id is the mandatory attribute")
        UUID id,

        @NotBlank(message = "Name is the mandatory attribute")
        @Size(max = 100, message = "Name length must be less than 100 characters")
        String name,

        @NotBlank(message = "Description is the mandatory attribute")
        @Size(max = 1000, message = "Description length must be less than 1000 characters")
        String description,

        @NotNull(message = "PriceDetails is the mandatory attribute")
        PriceDetailsDto priceDetails,

        @NotNull(message = "Quantity  is the mandatory attribute")
        Integer quantity,

        @NotNull(message = "Active is the mandatory attribute")
        Boolean active
) {
}

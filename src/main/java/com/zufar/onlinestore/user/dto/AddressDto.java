package com.zufar.onlinestore.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AddressDto(

        @NotBlank(message = "Line is the mandatory attribute")
        @Size(max = 55, message = "Line length must be less than 55 characters")
        String line,

        @NotBlank(message = "City is the mandatory attribute")
        @Size(max = 55, message = "City length must be less than 55 characters")
        String city,

        @NotBlank(message = "Country is the mandatory attribute")
        @Size(max = 55, message = "Country length must be less than 55 characters")
        String country
) {
}

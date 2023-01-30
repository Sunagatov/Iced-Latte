package com.zufar.onlinestore.customer.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {

    @NotBlank(message = "Line is mandatory")
    @Size(max = 55, message = "Line length must be less than 55 characters")
    private String line;

    @NotBlank(message = "City is mandatory")
    @Size(max = 55, message = "City length must be less than 55 characters")
    private String city;

    @NotBlank(message = "Country is mandatory")
    @Size(max = 55, message = "Country length must be less than 55 characters")
    private String country;
}

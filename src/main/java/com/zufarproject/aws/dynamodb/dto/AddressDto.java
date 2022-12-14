package com.zufarproject.aws.dynamodb.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
    @NotBlank(message = "Line must be between 2 to 55 characters")
    @Size(min = 2, max = 55)
    private String line;
    @NotBlank(message = "City must be between 2 to 55 characters")
    @Size(min = 2, max = 55)
    private String city;
    @NotBlank(message = "Country must be between 2 to 55 characters")
    @Size(min = 2, max = 55)
    private String country;
}

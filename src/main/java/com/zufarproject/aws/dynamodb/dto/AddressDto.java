package com.zufarproject.aws.dynamodb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
    @NotBlank(message = "line is mandatory")
    private String line;
    @NotBlank(message = "city is mandatory")
    private String city;
    @NotBlank(message = "country is mandatory")
    private String country;
}

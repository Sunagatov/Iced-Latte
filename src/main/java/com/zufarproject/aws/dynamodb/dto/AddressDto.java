package com.zufarproject.aws.dynamodb.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
    @NotBlank
    private String line;
    @NotBlank
    private String city;
    @NotBlank
    private String country;
}

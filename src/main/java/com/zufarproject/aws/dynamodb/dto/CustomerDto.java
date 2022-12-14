package com.zufarproject.aws.dynamodb.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDto {
    @NotBlank
    private String customerId;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String email;
    @NotNull
    @Valid
    private AddressDto addressDto;
}

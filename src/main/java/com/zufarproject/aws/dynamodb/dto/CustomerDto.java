package com.zufarproject.aws.dynamodb.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDto {

    @NotBlank(message = "CustomerId is mandatory")
    @Size(min = 2, max = 55)
    private String customerId;

    @NotBlank(message = "FirstName must be between 2 to 55 characters")
    @Size(min = 2, max = 55)
    private String firstName;

    @NotBlank(message = "LastName must be between 2 to 55 characters")
    @Size(min = 2, max = 55)
    private String lastName;

    @NotBlank(message = "Email is mandatory")
    @Email
    private String email;

    @NotNull(message = "AddressDto is mandatory")
    @Valid
    private AddressDto addressDto;
}

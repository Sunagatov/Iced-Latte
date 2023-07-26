package com.zufar.onlinestore.security.dto.authentication;

import com.zufar.onlinestore.user.dto.AddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record RegistrationRequest(

        @NotBlank(message = "FirstName is the mandatory attribute")
        @Size(max = 55, message = "FirstName length must be less than 55 characters")
        String firstName,

        @NotBlank(message = "LastName is the mandatory attribute")
        @Size(max = 55, message = "LastName length must be less than 55 characters")
        String lastName,

        @NotBlank(message = "Username is the mandatory attribute")
        @Size(max = 55, message = "Username length must be less than 55 characters")
        String userName,

        @Email(message = "Email should be valid")
        @NotBlank(message = "Email is the mandatory attribute")
        String email,

        @NotBlank(message = "Email is the mandatory attribute")
        String password,

        @Valid
        @NotNull(message = "Address is mandatory")
        AddressDto address
) {
}

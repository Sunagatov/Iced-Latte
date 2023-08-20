package com.zufar.onlinestore.security.dto.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record UserRegistrationRequest(

        @NotBlank(message = "FirstName is the mandatory attribute")
        @Size(max = 55, message = "FirstName length must be less than 55 characters")
        String firstName,

        @NotBlank(message = "LastName is the mandatory attribute")
        @Size(max = 55, message = "LastName length must be less than 55 characters")
        String lastName,

        @NotBlank(message = "Username is the mandatory attribute")
        @Size(max = 55, message = "Username length must be less than 55 characters")
        String username,

        @Email(message = "Email should be valid")
        @NotBlank(message = "Email is the mandatory attribute")
        String email,

        @NotBlank(message = "Password is the mandatory attribute")
        @Size(max = 55, message = "Password length must be less than 55 characters")
        String password
) {
}

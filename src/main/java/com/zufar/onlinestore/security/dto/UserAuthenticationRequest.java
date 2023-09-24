package com.zufar.onlinestore.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserAuthenticationRequest(

        @NotBlank(message = "Username is the mandatory attribute")
        @Size(max = 55, message = "Username length must be less than 55 characters")
        String username,

        @NotBlank(message = "Password is the mandatory attribute")
        @Size(max = 55, message = "Password length must be less than 55 characters")
        String password
) {
}
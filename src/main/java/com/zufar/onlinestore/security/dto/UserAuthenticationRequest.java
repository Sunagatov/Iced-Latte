package com.zufar.onlinestore.security.dto;

import jakarta.validation.constraints.NotBlank;

public record UserAuthenticationRequest(

        @NotBlank(message = "Email is the mandatory attribute")
        String email,

        @NotBlank(message = "Password is the mandatory attribute")
        String password
) {
}
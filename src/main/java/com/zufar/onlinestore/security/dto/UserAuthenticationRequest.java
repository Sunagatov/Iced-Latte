package com.zufar.onlinestore.security.dto;

import jakarta.validation.constraints.NotBlank;

public record UserAuthenticationRequest(

        @NotBlank(message = "Username is the mandatory attribute")
        String username,

        @NotBlank(message = "Password is the mandatory attribute")
        String password
) {
}
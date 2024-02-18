package com.zufar.icedlatte.security.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(

        @NotBlank(message = "Email is the mandatory attribute")
        String email,
        @NotBlank(message = "Code is the mandatory attribute")
        String code,
        @NotBlank(message = "Password is the mandatory attribute")
        String password
) {
}
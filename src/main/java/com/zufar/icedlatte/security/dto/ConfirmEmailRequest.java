package com.zufar.icedlatte.security.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailRequest(
    @NotBlank(message = "Email is the mandatory attribute")
    String email,
    @NotBlank(message = "Token is the mandatory attribute")
    String token) {
}

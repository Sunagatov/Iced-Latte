package com.zufar.onlinestore.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UserDto(
        UUID userId,
        String firstName,
        String lastName,
        String username,
        String email,
        String password,
        AddressDto address
) {
}

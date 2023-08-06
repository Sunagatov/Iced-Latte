package com.zufar.onlinestore.user.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserDto(
        String firstName,
        String lastName,
        String username,
        String email,
        String password,
        AddressDto address
) {
}

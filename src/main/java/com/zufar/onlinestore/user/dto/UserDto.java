package com.zufar.onlinestore.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UserDto(
        @JsonIgnore UUID userId,
        String firstName,
        String lastName,
        String stripeCustomerToken,
        String username,
        String email,
        String password,
        AddressDto address
) {
}

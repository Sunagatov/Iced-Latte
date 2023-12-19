package com.zufar.icedlatte.email.dto;

import com.zufar.icedlatte.security.dto.UserRegistrationRequest;

public record EmailConformationDto(
        String token,
        UserRegistrationRequest userRegistrationRequest) {}

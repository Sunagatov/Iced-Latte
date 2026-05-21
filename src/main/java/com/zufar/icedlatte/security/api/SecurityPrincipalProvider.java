package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserDto;

import java.util.UUID;

public interface SecurityPrincipalProvider {

    UserDto get();

    UUID getUserId();
}

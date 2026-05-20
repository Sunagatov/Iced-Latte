package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.exception.UserNotFoundException;

import java.util.UUID;

public interface UserLookupApi {

    UserDto getUserById(UUID userId) throws UserNotFoundException;

    UserDto getUserByEmail(String email) throws UserNotFoundException;
}

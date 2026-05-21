package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;
import com.zufar.icedlatte.user.exception.UserNotFoundException;

import java.util.UUID;

public interface UserLookupApi {

    UserLookupSnapshot getUserById(UUID userId) throws UserNotFoundException;

    UserLookupSnapshot getUserByEmail(String email) throws UserNotFoundException;
}

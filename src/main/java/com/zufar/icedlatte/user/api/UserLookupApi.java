package com.zufar.icedlatte.user.api;

import java.util.UUID;

import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;
import com.zufar.icedlatte.user.exception.UserNotFoundException;

public interface UserLookupApi {

    UserLookupSnapshot getUserById(UUID userId) throws UserNotFoundException;

    UserLookupSnapshot getUserByEmail(String email) throws UserNotFoundException;
}

package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.user.exception.UserNotFoundException;

public interface UserAuthenticationApi {

    UserAuthenticationSnapshot getUserAuthenticationByEmail(String email) throws UserNotFoundException;
}

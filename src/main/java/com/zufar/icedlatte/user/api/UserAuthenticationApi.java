package com.zufar.icedlatte.user.api;

import java.util.Optional;

public interface UserAuthenticationApi {

    Optional<UserAuthenticationSnapshot> findUserAuthenticationByEmail(String email);
}

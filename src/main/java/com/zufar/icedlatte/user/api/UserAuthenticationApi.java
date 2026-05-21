package com.zufar.icedlatte.user.api;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthenticationApi {

    Optional<UserAuthenticationSnapshot> findUserAuthenticationById(UUID userId);

    Optional<UserAuthenticationSnapshot> findUserAuthenticationByEmail(String email);
}

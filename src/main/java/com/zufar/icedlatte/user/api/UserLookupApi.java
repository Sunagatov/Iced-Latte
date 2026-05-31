package com.zufar.icedlatte.user.api;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;

public interface UserLookupApi {

    UserLookupSnapshot getUserById(UUID userId);

    Set<UserLookupSnapshot> getUsersByIds(Set<UUID> userIds);

    Optional<UserLookupSnapshot> findUserByEmail(String email);

    UserLookupSnapshot getUserByEmail(String email);
}

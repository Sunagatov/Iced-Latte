package com.zufar.icedlatte.user.api;

import java.util.List;
import java.util.UUID;

public record UserAuthenticationSnapshot(
        UUID userId,
        String email,
        String passwordHash,
        List<String> authorities,
        boolean accountNonExpired,
        boolean accountNonLocked,
        boolean credentialsNonExpired,
        boolean enabled) {}

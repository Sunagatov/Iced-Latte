package com.zufar.icedlatte.user.api;

import java.util.List;
import java.util.UUID;

/**
 * Authentication-only user view for the security module. The encoded password is intentionally exposed only through
 * this contract so Spring Security can verify credentials; do not reuse this record for profile or public user
 * responses.
 */
public record UserAuthenticationSnapshot(
        UUID userId,
        String email,
        String encodedPassword,
        List<String> authorities,
        boolean accountNonExpired,
        boolean accountNonLocked,
        boolean credentialsNonExpired,
        boolean enabled) {}

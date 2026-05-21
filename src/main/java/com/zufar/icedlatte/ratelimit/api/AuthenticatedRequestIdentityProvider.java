package com.zufar.icedlatte.ratelimit.api;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface AuthenticatedRequestIdentityProvider {

    Optional<String> findIdentity(HttpServletRequest request);
}

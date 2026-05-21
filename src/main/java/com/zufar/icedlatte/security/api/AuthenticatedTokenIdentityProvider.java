package com.zufar.icedlatte.security.api;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface AuthenticatedTokenIdentityProvider {

    Optional<String> findAccessTokenEmail(HttpServletRequest request);
}

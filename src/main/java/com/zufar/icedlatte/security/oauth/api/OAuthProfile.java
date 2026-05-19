package com.zufar.icedlatte.security.oauth.api;

import java.util.Objects;

public record OAuthProfile(String providerSubject,
                           String email,
                           boolean emailVerified,
                           String firstName,
                           String lastName) {
    public OAuthProfile {
        Objects.requireNonNull(providerSubject, "providerSubject");
        Objects.requireNonNull(email, "email");
    }
}

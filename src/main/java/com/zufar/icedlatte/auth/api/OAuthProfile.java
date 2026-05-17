package com.zufar.icedlatte.auth.api;

public record OAuthProfile(String providerSubject,
                           String email,
                           boolean emailVerified,
                           String firstName,
                           String lastName) {
}

package com.zufar.icedlatte.security.oauth.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum OAuthProvider {
    GOOGLE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String callbackPath() {
        return "/auth/" + id() + "/callback";
    }

    public static Optional<OAuthProvider> fromId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(provider -> provider.id().equalsIgnoreCase(id))
                .findFirst();
    }
}

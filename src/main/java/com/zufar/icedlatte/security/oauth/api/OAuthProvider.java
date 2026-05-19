package com.zufar.icedlatte.security.oauth.api;

import java.util.Arrays;
import java.util.Optional;

public enum OAuthProvider {

    GOOGLE("google");

    private final String id;

    OAuthProvider(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String callbackPath() {
        return "/auth/" + id + "/callback";
    }

    public static Optional<OAuthProvider> fromId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(provider -> provider.id.equalsIgnoreCase(id))
                .findFirst();
    }
}

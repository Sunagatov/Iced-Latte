package com.zufar.icedlatte.security.api.dto;

import java.util.UUID;

public record CurrentUserSnapshot(UUID id, String email, String displayName) {

    public CurrentUserSnapshot(UUID id, String email) {
        this(id, email, "");
    }

    public CurrentUserSnapshot {
        displayName = displayName.trim();
    }
}

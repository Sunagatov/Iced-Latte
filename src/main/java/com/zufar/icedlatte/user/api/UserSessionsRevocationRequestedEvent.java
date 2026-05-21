package com.zufar.icedlatte.user.api;

import java.util.UUID;

public record UserSessionsRevocationRequestedEvent(UUID userId) {
}

package com.zufar.icedlatte.user.api;

import java.util.UUID;

/** Published synchronously before deleting the user's dependent data. */
public record UserDeletionRequestedEvent(UUID userId) {}

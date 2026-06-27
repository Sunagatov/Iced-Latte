package com.zufar.icedlatte.user.service;

import org.jspecify.annotations.Nullable;

public record AvatarUploadCompletionCommand(
        @Nullable AvatarUploadSourceObject sourceObject,
        @Nullable String processedBucket,
        @Nullable String processedKey,
        @Nullable String contentType,
        @Nullable Integer width,
        @Nullable Integer height,
        @Nullable Long originalSizeBytes,
        @Nullable Long processedSizeBytes,
        @Nullable String sha256) {}

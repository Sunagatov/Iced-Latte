package com.zufar.icedlatte.user.service;

public record AvatarUploadCompletionCommand(
        AvatarUploadSourceObject sourceObject,
        String processedBucket,
        String processedKey,
        String contentType,
        Integer width,
        Integer height,
        Long originalSizeBytes,
        Long processedSizeBytes,
        String sha256) {}

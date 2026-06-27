package com.zufar.icedlatte.user.service;

public record AvatarUploadCompletion(
        ValidAvatarUploadSourceObject source,
        String processedBucket,
        String processedKey,
        String contentType,
        int width,
        int height,
        long originalSizeBytes,
        long processedSizeBytes,
        String sha256) {}

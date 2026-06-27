package com.zufar.icedlatte.user.service;

import java.util.UUID;

public record ValidAvatarUploadSourceObject(
        String bucket, String key, UUID userId, UUID uploadId, String requestedContentType) {}

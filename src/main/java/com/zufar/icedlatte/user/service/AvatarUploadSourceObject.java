package com.zufar.icedlatte.user.service;

import java.util.Map;

import org.jspecify.annotations.Nullable;

public record AvatarUploadSourceObject(
        @Nullable String bucket,
        @Nullable String key,
        @Nullable Map<String, String> metadata) {}

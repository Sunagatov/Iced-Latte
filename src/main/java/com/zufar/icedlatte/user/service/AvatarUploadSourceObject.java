package com.zufar.icedlatte.user.service;

import java.util.Map;

public record AvatarUploadSourceObject(String bucket, String key, Map<String, String> metadata) {}

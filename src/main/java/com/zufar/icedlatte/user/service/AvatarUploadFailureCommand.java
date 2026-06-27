package com.zufar.icedlatte.user.service;

public record AvatarUploadFailureCommand(
        AvatarUploadSourceObject sourceObject, String failureCode, String failureMessage) {}

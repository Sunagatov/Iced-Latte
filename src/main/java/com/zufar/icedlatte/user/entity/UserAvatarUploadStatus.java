package com.zufar.icedlatte.user.entity;

public enum UserAvatarUploadStatus {
    PENDING_UPLOAD,
    @SuppressWarnings("unused")
    UPLOADED,
    PROCESSING,
    READY,
    FAILED,
    EXPIRED,
    SUPERSEDED
}

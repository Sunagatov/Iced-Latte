package com.zufar.icedlatte.user.service;

import com.zufar.icedlatte.openapi.dto.AvatarUploadTargetResponse;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;

public interface AvatarUploadPresigner {

    AvatarUploadTargetResponse presign(UserAvatarUpload upload);
}

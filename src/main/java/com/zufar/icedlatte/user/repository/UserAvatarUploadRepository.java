package com.zufar.icedlatte.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zufar.icedlatte.user.entity.UserAvatarUpload;

public interface UserAvatarUploadRepository extends JpaRepository<UserAvatarUpload, UUID> {

    Optional<UserAvatarUpload> findByUserIdAndClientIdempotencyKey(UUID userId, String clientIdempotencyKey);
}

package com.zufar.icedlatte.user.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.entity.UserAvatarUploadStatus;

public interface UserAvatarUploadRepository extends JpaRepository<UserAvatarUpload, UUID> {

    java.util.Collection<UserAvatarUpload> findByStatusInAndExpiresAtBefore(
            java.util.Collection<UserAvatarUploadStatus> statuses, Instant expiresAt);

    Optional<UserAvatarUpload> findByUserIdAndClientIdempotencyKey(UUID userId, String clientIdempotencyKey);

    Optional<UserAvatarUpload> findByUserIdAndActiveTrue(UUID userId);

    boolean existsByUserIdAndCreatedAtAfterAndStatusIn(
            UUID userId, Instant createdAt, Collection<UserAvatarUploadStatus> statuses);

    Collection<UserAvatarUpload> findByUserIdAndStatusIn(UUID userId, Collection<UserAvatarUploadStatus> statuses);
}

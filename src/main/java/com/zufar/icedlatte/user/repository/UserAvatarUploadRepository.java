package com.zufar.icedlatte.user.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.entity.UserAvatarUploadStatus;

public interface UserAvatarUploadRepository extends JpaRepository<UserAvatarUpload, UUID> {

    @Query("select u.userId from UserAvatarUpload u where u.id = :id")
    Optional<UUID> findOwnerIdById(@Param("id") UUID id);

    long countByUserIdAndExpiresAtAfter(UUID userId, Instant now);

    @Query(
            "select u.id from UserAvatarUpload u where u.sourceCleanupScheduled = false and u.expiresAt < :now order by u.id")
    List<UUID> findSourceCleanupIds(@Param("now") Instant now, Pageable pageable);

    @Query(
            "select u.id from UserAvatarUpload u where u.status = :status and u.active = false and u.expiresAt < :now order by u.id")
    List<UUID> findAbandonedReadyIds(
            @Param("status") UserAvatarUploadStatus status, @Param("now") Instant now, Pageable pageable);

    @Query("select u.id from UserAvatarUpload u where u.status in :statuses and u.expiresAt < :now order by u.id")
    List<UUID> findExpiredIds(
            @Param("statuses") Collection<UserAvatarUploadStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);

    java.util.Collection<UserAvatarUpload> findByStatusInAndExpiresAtBefore(
            java.util.Collection<UserAvatarUploadStatus> statuses, Instant expiresAt);

    Optional<UserAvatarUpload> findByUserIdAndClientIdempotencyKey(UUID userId, String clientIdempotencyKey);

    Optional<UserAvatarUpload> findByUserIdAndActiveTrue(UUID userId);

    boolean existsByUserIdAndCreatedAtAfterAndStatusIn(
            UUID userId, Instant createdAt, Collection<UserAvatarUploadStatus> statuses);

    Collection<UserAvatarUpload> findByUserIdAndStatusIn(UUID userId, Collection<UserAvatarUploadStatus> statuses);
}

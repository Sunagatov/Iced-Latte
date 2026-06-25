package com.zufar.icedlatte.user.service;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.entity.UserAvatarUploadStatus;
import com.zufar.icedlatte.user.repository.UserAvatarUploadRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AvatarUploadLifecycleService {

    private static final String INCOMING_KEY_TEMPLATE = "avatars/incoming/%s/%s/source";

    private final UserAvatarUploadRepository repository;
    private final AvatarUploadProperties properties;
    private final Clock clock;
    private final Supplier<UUID> uploadIdSupplier;

    public AvatarUploadLifecycleService(UserAvatarUploadRepository repository, AvatarUploadProperties properties) {
        this(repository, properties, Clock.systemUTC(), UUID::randomUUID);
    }

    AvatarUploadLifecycleService(
            UserAvatarUploadRepository repository,
            AvatarUploadProperties properties,
            Clock clock,
            Supplier<UUID> uploadIdSupplier) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
        this.uploadIdSupplier = uploadIdSupplier;
    }

    @Transactional
    public UserAvatarUpload createPendingUpload(
            UUID userId, String contentType, long originalSizeBytes, String idempotencyKey) {
        var now = clock.instant();
        return repository
                .findByUserIdAndClientIdempotencyKey(userId, idempotencyKey)
                .map(existing -> existing.reusableAt(now) ? existing : rejectExpiredIntent(userId))
                .orElseGet(() ->
                        repository.save(newPendingUpload(userId, contentType, originalSizeBytes, idempotencyKey)));
    }

    private UserAvatarUpload rejectExpiredIntent(UUID userId) {
        log.info("avatar.upload_intent.expired_idempotency_retry: userId={}", userId);
        throw new BadRequestException(
                "Previous avatar upload intent expired. Please retry with a new Idempotency-Key.");
    }

    private UserAvatarUpload newPendingUpload(
            UUID userId, String contentType, long originalSizeBytes, String idempotencyKey) {
        var uploadId = uploadIdSupplier.get();
        var now = clock.instant();
        return UserAvatarUpload.builder()
                .id(uploadId)
                .userId(userId)
                .status(UserAvatarUploadStatus.PENDING_UPLOAD)
                .originalBucket(properties.incomingBucket())
                .originalKey(INCOMING_KEY_TEMPLATE.formatted(userId, uploadId))
                .contentType(contentType)
                .originalSizeBytes(originalSizeBytes)
                .clientIdempotencyKey(idempotencyKey)
                .active(false)
                .createdAt(now)
                .expiresAt(now.plus(properties.presignedUrlTtl()))
                .build();
    }
}

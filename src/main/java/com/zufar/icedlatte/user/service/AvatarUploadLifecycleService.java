package com.zufar.icedlatte.user.service;

import java.time.Clock;
import java.util.EnumSet;
import java.util.Optional;
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

    private static final int FAILURE_CODE_MAX_LENGTH = 64;
    private static final int FAILURE_MESSAGE_MAX_LENGTH = 512;
    private static final EnumSet<UserAvatarUploadStatus> DELETE_INVALIDATED_STATUSES = EnumSet.of(
            UserAvatarUploadStatus.PENDING_UPLOAD, UserAvatarUploadStatus.PROCESSING, UserAvatarUploadStatus.READY);

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

    @Transactional
    public Optional<UserAvatarUpload> markProcessing(AvatarUploadProcessingResult result) {
        ValidAvatarUploadSourceObject source = result.source();
        return repository.findById(source.uploadId()).flatMap(upload -> markProcessing(upload, result));
    }

    @Transactional
    public Optional<UserAvatarUpload> markFailed(UUID uploadId, String failureCode, String failureMessage) {
        return repository.findById(uploadId).map(upload -> markFailed(upload, failureCode, failureMessage));
    }

    @Transactional
    public Optional<UserAvatarUpload> markReady(AvatarUploadCompletion completion) {
        ValidAvatarUploadSourceObject source = completion.source();
        return repository.findById(source.uploadId()).flatMap(upload -> markReady(upload, completion));
    }

    @Transactional
    public void invalidateUserUploadsAfterAvatarDelete(UUID userId) {
        var uploads = repository.findByUserIdAndStatusIn(userId, DELETE_INVALIDATED_STATUSES);
        var now = clock.instant();
        uploads.forEach(upload -> {
            upload.setActive(false);
            upload.setStatus(UserAvatarUploadStatus.SUPERSEDED);
            upload.setSupersededAt(now);
        });
        repository.saveAll(uploads);
    }

    private Optional<UserAvatarUpload> markProcessing(UserAvatarUpload upload, AvatarUploadProcessingResult result) {
        ValidAvatarUploadSourceObject source = result.source();
        if (!matchesSource(upload, source)) {
            log.warn("avatar.upload_processing.ignored: reason=source_mismatch, uploadId={}", source.uploadId());
            return Optional.empty();
        }
        if (upload.getStatus() == UserAvatarUploadStatus.PROCESSING) {
            return Optional.of(upload);
        }
        if (upload.getStatus() != UserAvatarUploadStatus.PENDING_UPLOAD) {
            log.info(
                    "avatar.upload_processing.ignored: reason=status, uploadId={}, status={}",
                    upload.getId(),
                    upload.getStatus());
            return Optional.empty();
        }

        var now = clock.instant();
        if (!upload.getExpiresAt().isAfter(now)) {
            upload.setStatus(UserAvatarUploadStatus.EXPIRED);
            repository.save(upload);
            return Optional.empty();
        }

        AvatarImageInspection image = result.image();
        upload.setStatus(UserAvatarUploadStatus.PROCESSING);
        upload.setUploadedAt(now);
        upload.setContentType(image.contentType());
        upload.setOriginalSizeBytes(image.sizeBytes());
        upload.setImageWidth(image.width());
        upload.setImageHeight(image.height());
        return Optional.of(repository.save(upload));
    }

    private UserAvatarUpload markFailed(UserAvatarUpload upload, String failureCode, String failureMessage) {
        if (upload.getStatus() == UserAvatarUploadStatus.FAILED) {
            return upload;
        }
        if (upload.getStatus() == UserAvatarUploadStatus.READY
                || upload.getStatus() == UserAvatarUploadStatus.SUPERSEDED) {
            log.info(
                    "avatar.upload_failure.ignored: reason=status, uploadId={}, status={}",
                    upload.getId(),
                    upload.getStatus());
            return upload;
        }

        upload.setStatus(UserAvatarUploadStatus.FAILED);
        upload.setProcessedAt(clock.instant());
        upload.setFailureCode(truncate(failureCode, FAILURE_CODE_MAX_LENGTH));
        upload.setFailureMessage(truncate(failureMessage, FAILURE_MESSAGE_MAX_LENGTH));
        return repository.save(upload);
    }

    private Optional<UserAvatarUpload> markReady(UserAvatarUpload upload, AvatarUploadCompletion completion) {
        ValidAvatarUploadSourceObject source = completion.source();
        if (!matchesSource(upload, source)) {
            log.warn("avatar.upload_ready.ignored: reason=source_mismatch, uploadId={}", source.uploadId());
            return Optional.empty();
        }
        if (upload.getStatus() == UserAvatarUploadStatus.READY) {
            return Optional.of(upload);
        }
        if (upload.getStatus() != UserAvatarUploadStatus.PENDING_UPLOAD
                && upload.getStatus() != UserAvatarUploadStatus.PROCESSING) {
            log.info(
                    "avatar.upload_ready.ignored: reason=status, uploadId={}, status={}",
                    upload.getId(),
                    upload.getStatus());
            return Optional.empty();
        }

        upload.setStatus(UserAvatarUploadStatus.READY);
        upload.setProcessedBucket(completion.processedBucket());
        upload.setProcessedKey(completion.processedKey());
        upload.setContentType(completion.contentType());
        upload.setOriginalSizeBytes(completion.originalSizeBytes());
        upload.setProcessedSizeBytes(completion.processedSizeBytes());
        upload.setImageWidth(completion.width());
        upload.setImageHeight(completion.height());
        upload.setSha256(completion.sha256());
        upload.setProcessedAt(clock.instant());
        return Optional.of(repository.save(upload));
    }

    private boolean matchesSource(UserAvatarUpload upload, ValidAvatarUploadSourceObject source) {
        return upload.getUserId().equals(source.userId())
                && upload.getOriginalBucket().equals(source.bucket())
                && upload.getOriginalKey().equals(source.key());
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
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
                .originalKey(AvatarUploadStorageLayout.incomingKey(userId, uploadId))
                .contentType(contentType)
                .originalSizeBytes(originalSizeBytes)
                .clientIdempotencyKey(idempotencyKey)
                .active(false)
                .createdAt(now)
                .expiresAt(now.plus(properties.presignedUrlTtl()))
                .build();
    }
}

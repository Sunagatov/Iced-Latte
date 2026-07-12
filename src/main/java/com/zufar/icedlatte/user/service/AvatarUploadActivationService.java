package com.zufar.icedlatte.user.service;

import java.time.Clock;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.filestorage.api.FileStorageWriterApi;
import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.entity.UserAvatarUploadStatus;
import com.zufar.icedlatte.user.repository.UserAvatarUploadRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AvatarUploadActivationService {

    private static final EnumSet<UserAvatarUploadStatus> NEWER_UPLOAD_STATUSES = EnumSet.of(
            UserAvatarUploadStatus.PENDING_UPLOAD, UserAvatarUploadStatus.PROCESSING, UserAvatarUploadStatus.READY);

    private final UserAvatarUploadRepository repository;
    private final FileStorageWriterApi fileStorageWriterApi;
    private final Clock clock;

    @SuppressWarnings("unused")
    @Autowired
    public AvatarUploadActivationService(
            UserAvatarUploadRepository repository, FileStorageWriterApi fileStorageWriterApi) {
        this(repository, fileStorageWriterApi, Clock.systemUTC());
    }

    AvatarUploadActivationService(
            UserAvatarUploadRepository repository, FileStorageWriterApi fileStorageWriterApi, Clock clock) {
        this.repository = repository;
        this.fileStorageWriterApi = fileStorageWriterApi;
        this.clock = clock;
    }

    @Transactional
    public Optional<UserAvatarUpload> activate(UUID uploadId) {
        return repository.findById(uploadId).flatMap(this::activate);
    }

    private Optional<UserAvatarUpload> activate(UserAvatarUpload upload) {
        if (upload.isActive()) {
            return Optional.of(upload);
        }
        if (upload.getStatus() != UserAvatarUploadStatus.READY) {
            log.info(
                    "avatar.upload_activation.ignored: reason=status, uploadId={}, status={}",
                    upload.getId(),
                    upload.getStatus());
            return Optional.empty();
        }
        if (!StringUtils.hasText(upload.getProcessedBucket()) || !StringUtils.hasText(upload.getProcessedKey())) {
            log.warn("avatar.upload_activation.ignored: reason=missing_processed_object, uploadId={}", upload.getId());
            return Optional.empty();
        }

        Optional<UserAvatarUpload> activeUpload = repository.findByUserIdAndActiveTrue(upload.getUserId());
        if (activeUpload
                .filter(active -> active.getCreatedAt().isAfter(upload.getCreatedAt()))
                .isPresent()) {
            log.info("avatar.upload_activation.ignored: reason=newer_active_upload, uploadId={}", upload.getId());
            return Optional.empty();
        }
        if (repository.existsByUserIdAndCreatedAtAfterAndStatusIn(
                upload.getUserId(), upload.getCreatedAt(), NEWER_UPLOAD_STATUSES)) {
            log.info("avatar.upload_activation.ignored: reason=newer_upload, uploadId={}", upload.getId());
            return Optional.empty();
        }

        fileStorageWriterApi.recordExisting(
                new FileMetadataDto(upload.getUserId(), upload.getProcessedBucket(), upload.getProcessedKey()));

        activeUpload.filter(active -> !active.getId().equals(upload.getId())).ifPresent(this::supersede);
        upload.setActive(true);
        upload.setActivatedAt(clock.instant());
        return Optional.of(repository.save(upload));
    }

    private void supersede(UserAvatarUpload upload) {
        upload.setActive(false);
        upload.setStatus(UserAvatarUploadStatus.SUPERSEDED);
        upload.setSupersededAt(clock.instant());
        repository.saveAndFlush(upload);
    }
}

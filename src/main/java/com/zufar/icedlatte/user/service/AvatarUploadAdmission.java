package com.zufar.icedlatte.user.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.zufar.icedlatte.ratelimit.api.DailyOperationQuota;
import com.zufar.icedlatte.user.config.AvatarUsageProperties;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.repository.UserAvatarUploadRepository;
import com.zufar.icedlatte.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(AvatarUsageProperties.class)
public class AvatarUploadAdmission {
    private final UserRepository users;
    private final UserAvatarUploadRepository uploads;
    private final DailyOperationQuota quota;
    private final AvatarUsageProperties properties;

    public void lockUser(UUID userId) {
        users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    public Optional<UserAvatarUpload> lockUpload(UUID uploadId) {
        // Read only the owner ID before locking, so JPA does not cache stale upload state.
        return uploads.findOwnerIdById(uploadId).flatMap(userId -> {
            lockUser(userId);
            return uploads.findById(uploadId);
        });
    }

    public void checkLiveIntents(UUID userId, Instant now) {
        // Cancelled/superseded URLs can still write until their original expiry.
        if (uploads.countByUserIdAndExpiresAtAfter(userId, now) >= properties.liveIntentsPerUser()) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many live upload links. Wait for them to expire.");
        }
    }

    public void reserve(UUID userId) {
        quota.reserve("avatar:user:" + userId, properties.dailyUploadsPerUser());
        quota.reserve("avatar:global", properties.dailyUploads());
    }
}

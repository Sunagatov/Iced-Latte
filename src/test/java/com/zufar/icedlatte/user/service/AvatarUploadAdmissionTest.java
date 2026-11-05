package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.zufar.icedlatte.ratelimit.api.DailyOperationQuota;
import com.zufar.icedlatte.user.config.AvatarUsageProperties;
import com.zufar.icedlatte.user.entity.UserAvatarUpload;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserAvatarUploadRepository;
import com.zufar.icedlatte.user.repository.UserRepository;

class AvatarUploadAdmissionTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserAvatarUploadRepository uploads = mock(UserAvatarUploadRepository.class);
    private final DailyOperationQuota quota = mock(DailyOperationQuota.class);
    private final AvatarUploadAdmission admission =
            new AvatarUploadAdmission(users, uploads, quota, new AvatarUsageProperties(100, 10, 3));

    @Test
    void countsEveryUnexpiredIssuedKeyEvenIfItsIntentWasCancelledOrSuperseded() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        when(uploads.countByUserIdAndExpiresAtAfter(userId, now)).thenReturn(3L);
        assertThatThrownBy(() -> admission.checkLiveIntents(userId, now)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(quota);
    }

    @Test
    void locksOwnerBeforeLoadingMutableUploadState() {
        UUID userId = UUID.randomUUID(), uploadId = UUID.randomUUID();
        var upload = new UserAvatarUpload();
        when(uploads.findOwnerIdById(uploadId)).thenReturn(Optional.of(userId));
        when(users.findByIdForUpdate(userId)).thenReturn(Optional.of(mock(UserEntity.class)));
        when(uploads.findById(uploadId)).thenReturn(Optional.of(upload));
        assertThat(admission.lockUpload(uploadId)).contains(upload);
        var order = inOrder(users, uploads);
        order.verify(uploads).findOwnerIdById(uploadId);
        order.verify(users).findByIdForUpdate(userId);
        order.verify(uploads).findById(uploadId);
    }

    @Test
    void appliesPerUserAndWholeApplicationAllowance() {
        UUID userId = UUID.randomUUID();
        admission.reserve(userId);
        verify(quota).reserve("avatar:user:" + userId, 10);
        verify(quota).reserve("avatar:global", 100);
    }
}

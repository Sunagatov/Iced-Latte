package com.zufar.icedlatte.user.service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.common.monitoring.SentryHandledExceptionReporter;
import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.filestorage.api.FileStorageWriterApi;
import com.zufar.icedlatte.filestorage.api.FileUrlResolverApi;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.ChangeUserPasswordRequest;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.api.UserAccessControlApi;
import com.zufar.icedlatte.user.api.UserSessionsRevocationRequestedEvent;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService implements UserAccessControlApi {

    private final SingleUserProvider singleUserProvider;
    private final UserRepository userRepository;
    private final UserDtoConverter userDtoConverter;
    private final FileUrlResolverApi fileUrlResolverApi;
    private final FileStorageWriterApi fileStorageWriterApi;
    private final AvatarUploadLifecycleService avatarUploadLifecycleService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final SentryHandledExceptionReporter sentryHandledExceptionReporter;

    public UserDto getProfile(UUID userId) {
        return toProfileDto(singleUserProvider.getUserEntityById(userId));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public UserDto updateProfile(UUID userId, UpdateUserAccountRequest request) {
        AddressDto addressDto = request.getAddress();
        PutUsersRequestValidator.validate(
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber(),
                request.getBirthDate(),
                addressDto);
        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        userDtoConverter.updateEntity(userEntity, request);
        return toProfileDto(userRepository.save(userEntity));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteProfile(UUID userId) {
        UserEntity userEntity = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.delete(userEntity);
        cleanUpDeletedProfileAfterCommit(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void changePassword(UUID userId, ChangeUserPasswordRequest request) {
        var userEntity = singleUserProvider.getUserEntityById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), userEntity.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect.");
        }
        changePassword(userId, request.getNewPassword());
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Override
    public void changePassword(UUID userId, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        int updatedRows = userRepository.changeUserPassword(encodedPassword, userId);
        if (updatedRows == 0) {
            throw new UserNotFoundException(userId);
        }
        requestSessionsRevocationAfterCommit(userId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public int lockAccount(String email) {
        String normalizedEmail = Objects.requireNonNull(EmailNormalizer.normalize(email), "email must not be null");
        return userRepository.setAccountNonLockedStatus(normalizedEmail, false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public int unlockAccount(String email) {
        String normalizedEmail = Objects.requireNonNull(EmailNormalizer.normalize(email), "email must not be null");
        return userRepository.setAccountNonLockedStatus(normalizedEmail, true);
    }

    public Optional<String> findAvatarLink(UUID userId) {
        return fileUrlResolverApi.findFileUrl(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteAvatar(UUID userId) {
        avatarUploadLifecycleService.invalidateUserUploadsAfterAvatarDelete(userId);
        fileStorageWriterApi.deleteFile(userId);
    }

    private UserDto toProfileDto(UserEntity userEntity) {
        UserDto userDto = userDtoConverter.toDto(userEntity);
        userDto.setAvatarLink(findAvatarLink(userEntity.getId()).orElse(null));
        return userDto;
    }

    private void requestSessionsRevocationAfterCommit(UUID userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishEvent(new UserSessionsRevocationRequestedEvent(userId));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new UserSessionsRevocationRequestedEvent(userId));
            }
        });
    }

    private void cleanUpDeletedProfileAfterCommit(UUID userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cleanUpDeletedProfile(userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cleanUpDeletedProfile(userId);
            }
        });
    }

    private void cleanUpDeletedProfile(UUID userId) {
        try {
            eventPublisher.publishEvent(new UserSessionsRevocationRequestedEvent(userId));
        } catch (RuntimeException ex) {
            sentryHandledExceptionReporter.capture(ex, scope -> {
                scope.setTag("component", "user-profile");
                scope.setTag("operation", "session-revocation-after-delete");
                scope.setExtra("userId", userId.toString());
            });
            log.warn("user.profile.session_revocation_after_delete_failed: userId={}", userId, ex);
        }
        try {
            fileStorageWriterApi.deleteFile(userId);
        } catch (RuntimeException ex) {
            sentryHandledExceptionReporter.capture(ex, scope -> {
                scope.setTag("component", "user-profile");
                scope.setTag("operation", "avatar-delete-after-delete");
                scope.setExtra("userId", userId.toString());
            });
            log.warn("user.profile.avatar_delete_after_delete_failed: userId={}", userId, ex);
        }
    }
}

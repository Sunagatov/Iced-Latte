package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.common.monitoring.SentryHandledExceptionReporter;
import com.zufar.icedlatte.filestorage.api.FileStorageWriterApi;
import com.zufar.icedlatte.filestorage.api.FileUrlResolverApi;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.ChangeUserPasswordRequest;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.api.UserSessionsRevocationRequestedEvent;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService unit tests")
class UserProfileServiceTest {

    @Mock
    private SingleUserProvider singleUserProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDtoConverter userDtoConverter;

    @Mock
    private FileUrlResolverApi fileUrlResolverApi;

    @Mock
    private FileStorageWriterApi fileStorageWriterApi;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SentryHandledExceptionReporter sentryHandledExceptionReporter;

    @InjectMocks
    private UserProfileService userProfileService;

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("returns converted dto with avatar link when user exists")
        void returnsConvertedDtoWithAvatarLink() {
            UUID userId = UUID.randomUUID();
            UserEntity userEntity = UserEntity.builder().id(userId).build();
            UserDto userDto = new UserDto();

            when(singleUserProvider.getUserEntityById(userId)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(userDto);
            when(fileUrlResolverApi.findFileUrl(userId)).thenReturn(Optional.of("https://cdn.example.com/avatar.jpg"));

            UserDto result = userProfileService.getProfile(userId);

            assertThat(result).isSameAs(userDto);
            assertThat(result.getAvatarLink()).isEqualTo("https://cdn.example.com/avatar.jpg");
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("validates, delegates mapping to converter, and returns dto")
        void updateUser_validRequest_updatesAndReturnsDto() {
            UUID userId = UUID.randomUUID();
            UserEntity userEntity = UserEntity.builder().id(userId).build();
            UserDto expectedDto = new UserDto();
            LocalDate birthDate = LocalDate.of(1990, 5, 20);

            AddressDto addressDto = new AddressDto();
            addressDto.setCountry("UK");
            addressDto.setCity("London");
            addressDto.setLine("1 Main St");
            addressDto.setPostcode("SW1A 1AA");

            UpdateUserAccountRequest request = new UpdateUserAccountRequest();
            request.setFirstName("Alice");
            request.setLastName("Smith");
            request.setPhoneNumber("+1234567890");
            request.setBirthDate(birthDate);
            request.setAddress(addressDto);

            when(singleUserProvider.getUserEntityById(userId)).thenReturn(userEntity);
            when(userRepository.save(userEntity)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(expectedDto);
            when(fileUrlResolverApi.findFileUrl(userId)).thenReturn(Optional.of("https://cdn.example.com/avatar.jpg"));

            UserDto result = userProfileService.updateProfile(userId, request);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getAvatarLink()).isEqualTo("https://cdn.example.com/avatar.jpg");
            verify(userDtoConverter).updateEntity(userEntity, request);
            verify(userRepository).save(userEntity);
        }

        @Test
        @DisplayName("handles null address without error")
        void updateUser_nullAddress_delegatesToConverter() {
            UUID userId = UUID.randomUUID();
            UserEntity userEntity = UserEntity.builder().id(userId).build();

            UpdateUserAccountRequest request = new UpdateUserAccountRequest();
            request.setFirstName("Bob");
            request.setLastName("Jones");
            request.setAddress(null);

            when(singleUserProvider.getUserEntityById(userId)).thenReturn(userEntity);
            when(userRepository.save(userEntity)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(new UserDto());
            when(fileUrlResolverApi.findFileUrl(userId)).thenReturn(Optional.empty());

            userProfileService.updateProfile(userId, request);

            verify(userDtoConverter).updateEntity(userEntity, request);
        }
    }

    @Test
    @DisplayName("deleteProfile deletes user and then requests post-delete cleanup")
    void deleteProfileDeletesUserAndRequestsCleanup() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder().id(userId).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        userProfileService.deleteProfile(userId);

        verify(userRepository).delete(userEntity);
        verify(eventPublisher).publishEvent(new UserSessionsRevocationRequestedEvent(userId));
        verify(fileStorageWriterApi).deleteFile(userId);
    }

    @Test
    @DisplayName("deleteProfile captures handled cleanup failures in Sentry")
    void deleteProfileCapturesHandledCleanupFailuresInSentry() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder().id(userId).build();
        RuntimeException revocationFailure = new IllegalStateException("redis unavailable");
        RuntimeException avatarFailure = new IllegalStateException("s3 unavailable");
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        doThrow(revocationFailure).when(eventPublisher).publishEvent(new UserSessionsRevocationRequestedEvent(userId));
        doThrow(avatarFailure).when(fileStorageWriterApi).deleteFile(userId);

        userProfileService.deleteProfile(userId);

        verify(sentryHandledExceptionReporter).capture(eq(revocationFailure), any());
        verify(sentryHandledExceptionReporter).capture(eq(avatarFailure), any());
    }

    @Test
    @DisplayName("deleteProfile throws when user is missing")
    void deleteProfileThrowsWhenUserIsMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.deleteProfile(userId)).isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).delete(any());
        verifyNoInteractions(eventPublisher, fileStorageWriterApi);
    }

    @Test
    @DisplayName("findAvatarLink delegates to file storage")
    void findAvatarLinkDelegatesToFileStorage() {
        UUID userId = UUID.randomUUID();
        Optional<String> avatarLink = Optional.of("https://cdn.example.com/avatar.jpg");
        when(fileUrlResolverApi.findFileUrl(userId)).thenReturn(avatarLink);

        assertThat(userProfileService.findAvatarLink(userId)).isEqualTo(avatarLink);
    }

    @Test
    @DisplayName("deleteAvatar delegates to file storage")
    void deleteAvatarDelegatesToFileStorage() {
        UUID userId = UUID.randomUUID();

        userProfileService.deleteAvatar(userId);

        verify(fileStorageWriterApi).deleteFile(userId);
    }

    @Nested
    @DisplayName("account access")
    class AccountAccess {

        @Test
        @DisplayName("lockAccount normalizes email before updating account state")
        void lockAccountNormalizesEmail() {
            when(userRepository.setAccountNonLockedStatus("user@example.com", false))
                    .thenReturn(1);

            int updatedRows = userProfileService.lockAccount(" User@Example.COM ");

            assertThat(updatedRows).isEqualTo(1);
            verify(userRepository).setAccountNonLockedStatus("user@example.com", false);
        }

        @Test
        @DisplayName("unlockAccount normalizes email before updating account state")
        void unlockAccountNormalizesEmail() {
            when(userRepository.setAccountNonLockedStatus("user@example.com", true))
                    .thenReturn(1);

            int updatedRows = userProfileService.unlockAccount(" User@Example.COM ");

            assertThat(updatedRows).isEqualTo(1);
            verify(userRepository).setAccountNonLockedStatus("user@example.com", true);
        }

        @Test
        @DisplayName("lockAccount rejects null email")
        @SuppressWarnings("DataFlowIssue")
        void lockAccountRejectsNullEmail() {
            assertThatThrownBy(() -> userProfileService.lockAccount(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("email must not be null");

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("unlockAccount rejects null email")
        @SuppressWarnings("DataFlowIssue")
        void unlockAccountRejectsNullEmail() {
            assertThatThrownBy(() -> userProfileService.unlockAccount(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("email must not be null");

            verifyNoInteractions(userRepository);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("changes password when old password matches")
        void changePassword_validOldPassword_updatesPassword() {
            UUID userId = UUID.randomUUID();
            UserEntity user = new UserEntity();
            user.setPassword("encoded_old");

            ChangeUserPasswordRequest request = new ChangeUserPasswordRequest();
            request.setOldPassword("old_plain");
            request.setNewPassword("new_plain");

            when(singleUserProvider.getUserEntityById(userId)).thenReturn(user);
            when(passwordEncoder.matches("old_plain", "encoded_old")).thenReturn(true);
            when(passwordEncoder.encode("new_plain")).thenReturn("encoded_new");
            when(userRepository.changeUserPassword("encoded_new", userId)).thenReturn(1);

            userProfileService.changePassword(userId, request);

            verify(userRepository).changeUserPassword("encoded_new", userId);
            verify(eventPublisher).publishEvent(new UserSessionsRevocationRequestedEvent(userId));
        }

        @Test
        @DisplayName("throws when old password does not match")
        void changePassword_wrongOldPassword_throwsUnauthorizedException() {
            UUID userId = UUID.randomUUID();
            UserEntity user = new UserEntity();
            user.setPassword("encoded_old");

            ChangeUserPasswordRequest request = new ChangeUserPasswordRequest();
            request.setOldPassword("wrong_plain");
            request.setNewPassword("new_plain");

            when(singleUserProvider.getUserEntityById(userId)).thenReturn(user);
            when(passwordEncoder.matches("wrong_plain", "encoded_old")).thenReturn(false);

            assertThatThrownBy(() -> userProfileService.changePassword(userId, request))
                    .isInstanceOf(UnauthorizedException.class);

            verify(userRepository, never()).changeUserPassword(any(), any());
        }

        @Test
        @DisplayName("direct overload encodes and saves")
        void changePassword_directOverload_encodesAndSaves() {
            UUID userId = UUID.randomUUID();
            when(passwordEncoder.encode("new_plain")).thenReturn("encoded_new");
            when(userRepository.changeUserPassword("encoded_new", userId)).thenReturn(1);

            userProfileService.changePassword(userId, "new_plain");

            verify(userRepository).changeUserPassword("encoded_new", userId);
            verify(eventPublisher).publishEvent(new UserSessionsRevocationRequestedEvent(userId));
        }

        @Test
        @DisplayName("direct overload requests session revocation only after commit")
        void changePasswordDirectOverloadRequestsSessionRevocationAfterCommit() {
            UUID userId = UUID.randomUUID();
            when(passwordEncoder.encode("new_plain")).thenReturn("encoded_new");
            when(userRepository.changeUserPassword("encoded_new", userId)).thenReturn(1);

            TransactionSynchronizationManager.initSynchronization();
            try {
                userProfileService.changePassword(userId, "new_plain");

                verify(eventPublisher, never()).publishEvent(any());

                TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> {
                    synchronization.beforeCommit(false);
                    synchronization.afterCommit();
                });

                verify(eventPublisher).publishEvent(new UserSessionsRevocationRequestedEvent(userId));
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("direct overload throws when password update touches no rows")
        void changePasswordDirectOverloadThrowsWhenUserIsMissing() {
            UUID userId = UUID.randomUUID();
            when(passwordEncoder.encode("new_plain")).thenReturn("encoded_new");
            when(userRepository.changeUserPassword("encoded_new", userId)).thenReturn(0);

            assertThatThrownBy(() -> userProfileService.changePassword(userId, "new_plain"))
                    .isInstanceOf(UserNotFoundException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}

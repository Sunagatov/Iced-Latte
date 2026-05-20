package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.filestorage.FileStorageService;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.ChangeUserPasswordRequest;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.security.api.session.AuthSessionService;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;
import com.zufar.icedlatte.user.validator.PutUsersRequestValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @SuppressWarnings("unused")
    private PutUsersRequestValidator putUsersRequestValidator;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthSessionService authSessionService;

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
            when(fileStorageService.findFileUrl(userId)).thenReturn(Optional.of("https://cdn.example.com/avatar.jpg"));

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
            when(fileStorageService.findFileUrl(userId)).thenReturn(Optional.of("https://cdn.example.com/avatar.jpg"));

            UserDto result = userProfileService.updateProfile(userId, request);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getAvatarLink()).isEqualTo("https://cdn.example.com/avatar.jpg");
            verify(putUsersRequestValidator).validate("Alice", "Smith", "+1234567890", birthDate, addressDto);
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
            when(fileStorageService.findFileUrl(userId)).thenReturn(Optional.empty());

            userProfileService.updateProfile(userId, request);

            verify(putUsersRequestValidator).validate("Bob", "Jones", null, null, null);
            verify(userDtoConverter).updateEntity(userEntity, request);
        }
    }

    @Test
    @DisplayName("deleteProfile delegates to repository")
    void deleteProfileDelegatesToRepository() {
        UUID userId = UUID.randomUUID();

        userProfileService.deleteProfile(userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("findAvatarLink delegates to file storage")
    void findAvatarLinkDelegatesToFileStorage() {
        UUID userId = UUID.randomUUID();
        Optional<String> avatarLink = Optional.of("https://cdn.example.com/avatar.jpg");
        when(fileStorageService.findFileUrl(userId)).thenReturn(avatarLink);

        assertThat(userProfileService.findAvatarLink(userId)).isEqualTo(avatarLink);
    }

    @Test
    @DisplayName("deleteAvatar delegates to file storage")
    void deleteAvatarDelegatesToFileStorage() {
        UUID userId = UUID.randomUUID();

        userProfileService.deleteAvatar(userId);

        verify(fileStorageService).deleteFile(userId);
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

            userProfileService.changePassword(userId, request);

            verify(userRepository).changeUserPassword("encoded_new", userId);
            verify(authSessionService).revokeAllForUser(userId);
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

            userProfileService.changePassword(userId, "new_plain");

            verify(userRepository).changeUserPassword("encoded_new", userId);
            verify(authSessionService).revokeAllForUser(userId);
        }
    }
}

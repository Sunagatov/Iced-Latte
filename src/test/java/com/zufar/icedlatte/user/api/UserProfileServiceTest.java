package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.filestorage.file.FileProvider;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.converter.AddressDtoConverter;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.Address;
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

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    private AddressDtoConverter addressDtoConverter;
    @Mock
    @SuppressWarnings("unused")
    private PutUsersRequestValidator putUsersRequestValidator;
    @Mock
    private FileProvider fileProvider;

    @InjectMocks
    private UserProfileService userProfileService;

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfile {

        @Test
        @DisplayName("returns converted dto with avatar link when user exists")
        void returnsConvertedDtoWithAvatarLink() {
            UUID userId = UUID.randomUUID();
            UserEntity userEntity = UserEntity.builder().id(userId).build();
            UserDto userDto = new UserDto();

            when(singleUserProvider.getUserEntityById(userId)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(userDto);
            when(fileProvider.getRelatedObjectUrl(userId)).thenReturn(Optional.of("https://cdn.example.com/avatar.jpg"));

            UserDto result = userProfileService.getUserProfile(userId);

            assertThat(result).isSameAs(userDto);
            assertThat(result.getAvatarLink()).isEqualTo("https://cdn.example.com/avatar.jpg");
        }
    }

    @Nested
    @DisplayName("updateUserProfile")
    class UpdateUserProfile {

        @Test
        @DisplayName("updates user fields and returns dto")
        void updateUser_validRequest_updatesAndReturnsDto() {
            UUID userId = UUID.randomUUID();
            UserEntity userEntity = UserEntity.builder().id(userId).build();
            UserDto expectedDto = new UserDto();
            Address address = new Address();
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
            when(addressDtoConverter.toEntity(addressDto)).thenReturn(address);
            when(userRepository.save(userEntity)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(expectedDto);
            when(fileProvider.getRelatedObjectUrl(userId)).thenReturn(Optional.of("https://cdn.example.com/avatar.jpg"));

            UserDto result = userProfileService.updateUserProfile(userId, request);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getAvatarLink()).isEqualTo("https://cdn.example.com/avatar.jpg");
            assertThat(userEntity.getFirstName()).isEqualTo("Alice");
            assertThat(userEntity.getLastName()).isEqualTo("Smith");
            assertThat(userEntity.getPhoneNumber()).isEqualTo("+1234567890");
            assertThat(userEntity.getBirthDate()).isEqualTo(birthDate);
            assertThat(userEntity.getAddress()).isEqualTo(address);
            verify(putUsersRequestValidator).validate("Alice", "Smith", "+1234567890", birthDate, addressDto);
            var inOrder = inOrder(putUsersRequestValidator, singleUserProvider, addressDtoConverter, userRepository, userDtoConverter, fileProvider);
            inOrder.verify(putUsersRequestValidator).validate("Alice", "Smith", "+1234567890", birthDate, addressDto);
            inOrder.verify(singleUserProvider).getUserEntityById(userId);
            inOrder.verify(addressDtoConverter).toEntity(addressDto);
            inOrder.verify(userRepository).save(userEntity);
            inOrder.verify(userDtoConverter).toDto(userEntity);
            inOrder.verify(fileProvider).getRelatedObjectUrl(userId);
        }

        @Test
        @DisplayName("handles null address without conversion")
        void updateUser_nullAddress_setsNullAddress() {
            UUID userId = UUID.randomUUID();
            UserEntity userEntity = UserEntity.builder().id(userId).build();

            UpdateUserAccountRequest request = new UpdateUserAccountRequest();
            request.setFirstName("Bob");
            request.setLastName("Jones");
            request.setAddress(null);

            when(singleUserProvider.getUserEntityById(userId)).thenReturn(userEntity);
            when(userRepository.save(userEntity)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(new UserDto());
            when(fileProvider.getRelatedObjectUrl(userId)).thenReturn(Optional.empty());

            userProfileService.updateUserProfile(userId, request);

            assertThat(userEntity.getAddress()).isNull();
            verify(addressDtoConverter, never()).toEntity(org.mockito.ArgumentMatchers.any());
            verify(putUsersRequestValidator).validate("Bob", "Jones", null, null, null);
        }

        @Test
        @DisplayName("treats blank address payload as absent")
        void updateUser_blankAddressPayload_skipsAddressConversion() {
            UUID userId = UUID.randomUUID();
            UserEntity userEntity = UserEntity.builder()
                    .id(userId)
                    .address(new Address())
                    .build();

            AddressDto blankAddress = new AddressDto();
            blankAddress.setCountry(" ");
            blankAddress.setCity(null);
            blankAddress.setLine("");
            blankAddress.setPostcode("  ");

            UpdateUserAccountRequest request = new UpdateUserAccountRequest();
            request.setFirstName("Carol");
            request.setLastName("Jones");
            request.setPhoneNumber("+447700900000");
            request.setAddress(blankAddress);

            when(singleUserProvider.getUserEntityById(userId)).thenReturn(userEntity);
            when(userRepository.save(userEntity)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(new UserDto());
            when(fileProvider.getRelatedObjectUrl(userId)).thenReturn(Optional.empty());

            userProfileService.updateUserProfile(userId, request);

            assertThat(userEntity.getAddress()).isNull();
            verify(addressDtoConverter, never()).toEntity(blankAddress);
            verify(putUsersRequestValidator).validate("Carol", "Jones", "+447700900000", null, blankAddress);
        }

        @Test
        @DisplayName("converts partially filled address payloads")
        void updateUser_partialAddressPayload_convertsAddress() {
            UUID userId = UUID.randomUUID();
            UserEntity userEntity = UserEntity.builder().id(userId).build();
            AddressDto partialAddress = new AddressDto();
            partialAddress.setCity("London");
            Address mappedAddress = new Address();

            UpdateUserAccountRequest request = new UpdateUserAccountRequest();
            request.setFirstName("Dana");
            request.setLastName("Smith");
            request.setAddress(partialAddress);

            when(singleUserProvider.getUserEntityById(userId)).thenReturn(userEntity);
            when(addressDtoConverter.toEntity(partialAddress)).thenReturn(mappedAddress);
            when(userRepository.save(userEntity)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(new UserDto());
            when(fileProvider.getRelatedObjectUrl(userId)).thenReturn(Optional.empty());

            userProfileService.updateUserProfile(userId, request);

            assertThat(userEntity.getAddress()).isSameAs(mappedAddress);
            verify(addressDtoConverter).toEntity(partialAddress);
            verifyNoMoreInteractions(addressDtoConverter);
        }
    }

    @Test
    @DisplayName("deleteUserProfile delegates to repository")
    void deleteUserProfileDelegatesToRepository() {
        UUID userId = UUID.randomUUID();

        userProfileService.deleteUserProfile(userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    @DisplayName("getAvatarLink delegates to file provider")
    void getAvatarLinkDelegatesToFileProvider() {
        UUID userId = UUID.randomUUID();
        Optional<String> avatarLink = Optional.of("https://cdn.example.com/avatar.jpg");
        when(fileProvider.getRelatedObjectUrl(userId)).thenReturn(avatarLink);

        assertThat(userProfileService.getAvatarLink(userId)).isEqualTo(avatarLink);
    }
}

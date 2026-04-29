package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.UserRepository;
import com.zufar.icedlatte.user.stub.UserDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SingleUserProvider unit tests")
class SingleUserProviderTest {

    @Mock
    private UserRepository userCrudRepository;

    @Mock
    private UserDtoConverter userDtoConverter;

    @Mock
    private UserAvatarLinkUpdater userAvatarLinkUpdater;


    @InjectMocks
    private SingleUserProvider singleUserProvider;

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("returns converted dto when user exists")
        void returnsConvertedDtoWhenUserExists() {
            UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
            UserEntity testUserEntity = UserDtoTestStub.createUserEntity();

            when(userCrudRepository.findById(userId)).thenReturn(java.util.Optional.of(testUserEntity));

            UserDto expectedUserDto = UserDtoTestStub.createUserDto();
            when(userDtoConverter.toDto(testUserEntity)).thenReturn(expectedUserDto);
            when(userAvatarLinkUpdater.update(expectedUserDto)).thenReturn(expectedUserDto);

            UserDto actualUserDto = singleUserProvider.getUserById(userId);

            assertThat(actualUserDto).isEqualTo(expectedUserDto);
            verify(userCrudRepository).findById(userId);
            verify(userDtoConverter).toDto(testUserEntity);
            verify(userAvatarLinkUpdater).update(expectedUserDto);
        }

        @Test
        @DisplayName("throws when the user does not exist")
        void throwsWhenUserDoesNotExist() {
            UUID nonExistentUserId = UUID.randomUUID();

            when(userCrudRepository.findById(nonExistentUserId)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> singleUserProvider.getUserById(nonExistentUserId))
                    .isInstanceOf(UserNotFoundException.class);
            verify(userCrudRepository).findById(nonExistentUserId);
            verifyNoMoreInteractions(userDtoConverter, userAvatarLinkUpdater);
        }
    }

    @Test
    @DisplayName("getUserEntityById returns entity directly")
    void getUserEntityByIdReturnsEntityDirectly() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = UserDtoTestStub.createUserEntity();
        when(userCrudRepository.findById(userId)).thenReturn(java.util.Optional.of(entity));

        assertThat(singleUserProvider.getUserEntityById(userId)).isSameAs(entity);
        verify(userCrudRepository).findById(userId);
        verifyNoMoreInteractions(userDtoConverter, userAvatarLinkUpdater);
    }

    @Test
    @DisplayName("getUserEntityByEmail returns entity by email")
    void getUserEntityByEmailReturnsEntity() {
        UserEntity entity = UserDtoTestStub.createUserEntity();
        when(userCrudRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(entity));

        assertThat(singleUserProvider.getUserEntityByEmail("user@example.com")).isSameAs(entity);
        verify(userCrudRepository).findByEmail("user@example.com");
        verifyNoMoreInteractions(userDtoConverter, userAvatarLinkUpdater);
    }

    @Test
    @DisplayName("getUserEntityByEmail throws when user is missing")
    void getUserEntityByEmailThrowsWhenMissing() {
        when(userCrudRepository.findByEmail("missing@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> singleUserProvider.getUserEntityByEmail("missing@example.com"))
                .isInstanceOf(UserNotFoundException.class);
        verify(userCrudRepository).findByEmail("missing@example.com");
    }
}

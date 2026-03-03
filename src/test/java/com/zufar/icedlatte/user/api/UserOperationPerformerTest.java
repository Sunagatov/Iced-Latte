package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("User operation performer unit tests")
class UserOperationPerformerTest {

    @Nested
    @DisplayName("DeleteUserOperationPerformer")
    class DeleteTests {

        @Mock
        private UserRepository userRepository;
        @InjectMocks
        private DeleteUserOperationPerformer deleter;

        @Test
        @DisplayName("Delegates deleteById to repository")
        void deleteUser_callsRepositoryDeleteById() {
            UUID userId = UUID.randomUUID();

            deleter.deleteUser(userId);

            verify(userRepository).deleteById(userId);
        }
    }

    @Nested
    @DisplayName("UpdateUserOperationPerformer")
    class UpdateTests {

        @Mock
        private SingleUserProvider singleUserProvider;
        @Mock
        private UserRepository userCrudRepository;
        @Mock
        private UserDtoConverter userDtoConverter;
        @Mock
        private AddressDtoConverter addressDtoConverter;
        @Mock
        private SecurityPrincipalProvider securityPrincipalProvider;
        @Mock
        @SuppressWarnings("unused") // required by @InjectMocks, validate() is void — no stubbing needed
        private PutUsersRequestValidator putUsersRequestValidator;
        @InjectMocks
        private UpdateUserOperationPerformer updater;

        @Test
        @DisplayName("Updates user fields and returns DTO")
        void updateUser_validRequest_updatesAndReturnsDto() {
            UUID userId = UUID.randomUUID();
            UserEntity userEntity = UserEntity.builder().id(userId).build();
            UserDto expectedDto = new UserDto();
            Address address = new Address();

            UpdateUserAccountRequest request = new UpdateUserAccountRequest();
            request.setFirstName("Alice");
            request.setLastName("Smith");
            request.setPhoneNumber("+1234567890");
            request.setAddress(new AddressDto());

            when(securityPrincipalProvider.getUserId()).thenReturn(userId);
            when(singleUserProvider.getUserEntityById(userId)).thenReturn(userEntity);
            when(addressDtoConverter.toEntity(any(AddressDto.class))).thenReturn(address);
            when(userCrudRepository.save(userEntity)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(expectedDto);

            UserDto result = updater.updateUser(request);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(userEntity.getFirstName()).isEqualTo("Alice");
            assertThat(userEntity.getLastName()).isEqualTo("Smith");
            assertThat(userEntity.getPhoneNumber()).isEqualTo("+1234567890");
            assertThat(userEntity.getAddress()).isEqualTo(address);
        }

        @Test
        @DisplayName("Handles null address without NPE")
        void updateUser_nullAddress_setsNullAddress() {
            UUID userId = UUID.randomUUID();
            UserEntity userEntity = UserEntity.builder().id(userId).build();

            UpdateUserAccountRequest request = new UpdateUserAccountRequest();
            request.setFirstName("Bob");
            request.setLastName("Jones");
            request.setAddress(null);

            when(securityPrincipalProvider.getUserId()).thenReturn(userId);
            when(singleUserProvider.getUserEntityById(userId)).thenReturn(userEntity);
            when(addressDtoConverter.toEntity(null)).thenReturn(null);
            when(userCrudRepository.save(userEntity)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(new UserDto());

            updater.updateUser(request);

            assertThat(userEntity.getAddress()).isNull();
        }
    }
}

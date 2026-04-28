package com.zufar.icedlatte.user.api;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("User operation performer unit tests")
class UserOperationPerformerTest {


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
            when(userCrudRepository.save(userEntity)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(expectedDto);

            UserDto result = updater.updateUser(userId, request);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(userEntity.getFirstName()).isEqualTo("Alice");
            assertThat(userEntity.getLastName()).isEqualTo("Smith");
            assertThat(userEntity.getPhoneNumber()).isEqualTo("+1234567890");
            assertThat(userEntity.getBirthDate()).isEqualTo(birthDate);
            assertThat(userEntity.getAddress()).isEqualTo(address);
            verify(putUsersRequestValidator).validate("Alice", "Smith", "+1234567890", birthDate, addressDto);
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

            when(singleUserProvider.getUserEntityById(userId)).thenReturn(userEntity);
            when(userCrudRepository.save(userEntity)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(new UserDto());

            updater.updateUser(userId, request);

            assertThat(userEntity.getAddress()).isNull();
            verify(addressDtoConverter, never()).toEntity(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("Treats a blank address payload as absent and skips address conversion")
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
            when(userCrudRepository.save(userEntity)).thenReturn(userEntity);
            when(userDtoConverter.toDto(userEntity)).thenReturn(new UserDto());

            updater.updateUser(userId, request);

            assertThat(userEntity.getAddress()).isNull();
            verify(addressDtoConverter, never()).toEntity(blankAddress);
            verify(putUsersRequestValidator).validate("Carol", "Jones", "+447700900000", null, blankAddress);
        }
    }
}

package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressRequest;
import com.zufar.icedlatte.user.converter.DeliveryAddressDtoConverter;
import com.zufar.icedlatte.user.entity.DeliveryAddressEntity;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.exception.DeliveryAddressNotFoundException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryAddress service unit tests")
class DeliveryAddressServiceTest {

    @Nested
    @DisplayName("DeliveryAddressCreator")
    class CreatorTests {

        @Mock
        private DeliveryAddressRepository addressRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private DeliveryAddressDtoConverter converter;
        @InjectMocks
        private DeliveryAddressCreator creator;

        @Test
        @DisplayName("Creates address and marks it default when it is the first one")
        void create_firstAddress_isSetAsDefault() {
            UUID userId = UUID.randomUUID();
            UserEntity user = new UserEntity();
            DeliveryAddressRequest request = new DeliveryAddressRequest();
            DeliveryAddressEntity entity = new DeliveryAddressEntity();
            DeliveryAddressDto dto = new DeliveryAddressDto();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(converter.toEntity(request)).thenReturn(entity);
            when(addressRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(addressRepository.save(entity)).thenReturn(entity);
            when(converter.toDto(entity)).thenReturn(dto);

            DeliveryAddressDto result = creator.create(userId, request);

            assertThat(result).isEqualTo(dto);
            assertThat(entity.isDefault()).isTrue();
        }

        @Test
        @DisplayName("Creates address and does NOT mark it default when others exist")
        void create_subsequentAddress_isNotDefault() {
            UUID userId = UUID.randomUUID();
            UserEntity user = new UserEntity();
            DeliveryAddressRequest request = new DeliveryAddressRequest();
            DeliveryAddressEntity entity = new DeliveryAddressEntity();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(converter.toEntity(request)).thenReturn(entity);
            when(addressRepository.findAllByUserId(userId)).thenReturn(List.of(new DeliveryAddressEntity()));
            when(addressRepository.save(entity)).thenReturn(entity);
            when(converter.toDto(entity)).thenReturn(new DeliveryAddressDto());

            creator.create(userId, request);

            assertThat(entity.isDefault()).isFalse();
        }

        @Test
        @DisplayName("Throws UserNotFoundException when user does not exist")
        void create_userNotFound_throwsUserNotFoundException() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> creator.create(userId, new DeliveryAddressRequest()))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("DeliveryAddressProvider")
    class ProviderTests {

        @Mock
        private DeliveryAddressRepository repository;
        @Mock
        private DeliveryAddressDtoConverter converter;
        @InjectMocks
        private DeliveryAddressProvider provider;

        @Test
        @DisplayName("Returns mapped DTOs for all user addresses")
        void getAll_returnsAllAddresses() {
            UUID userId = UUID.randomUUID();
            DeliveryAddressEntity entity = new DeliveryAddressEntity();
            DeliveryAddressDto dto = new DeliveryAddressDto();
            when(repository.findAllByUserId(userId)).thenReturn(List.of(entity));
            when(converter.toDto(entity)).thenReturn(dto);

            List<DeliveryAddressDto> result = provider.getAll(userId);

            assertThat(result).containsExactly(dto);
        }

        @Test
        @DisplayName("Returns empty list when user has no addresses")
        void getAll_noAddresses_returnsEmpty() {
            UUID userId = UUID.randomUUID();
            when(repository.findAllByUserId(userId)).thenReturn(List.of());

            assertThat(provider.getAll(userId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("DeliveryAddressUpdater")
    class UpdaterTests {

        @Mock
        private DeliveryAddressRepository repository;
        @Mock
        private DeliveryAddressDtoConverter converter;
        @InjectMocks
        private DeliveryAddressUpdater updater;

        @Test
        @DisplayName("Updates all fields and returns DTO")
        void update_existingAddress_updatesFields() {
            UUID userId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            DeliveryAddressEntity entity = new DeliveryAddressEntity();
            DeliveryAddressRequest request = new DeliveryAddressRequest();
            request.setLabel("Home");
            request.setLine("1 Main St");
            request.setCity("London");
            request.setCountry("GB");
            request.setPostcode("SW1A 1AA");
            DeliveryAddressDto dto = new DeliveryAddressDto();

            when(repository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(entity));
            when(repository.save(entity)).thenReturn(entity);
            when(converter.toDto(entity)).thenReturn(dto);

            DeliveryAddressDto result = updater.update(userId, addressId, request);

            assertThat(result).isEqualTo(dto);
            assertThat(entity.getLabel()).isEqualTo("Home");
            assertThat(entity.getCity()).isEqualTo("London");
        }

        @Test
        @DisplayName("Throws DeliveryAddressNotFoundException when address not found")
        void update_notFound_throwsDeliveryAddressNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            when(repository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> updater.update(userId, addressId, new DeliveryAddressRequest()))
                    .isInstanceOf(DeliveryAddressNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("DeliveryAddressDeleter")
    class DeleterTests {

        @Mock
        private DeliveryAddressRepository repository;
        @InjectMocks
        private DeliveryAddressDeleter deleter;

        @Test
        @DisplayName("Deletes address when found")
        void delete_existingAddress_deletesIt() {
            UUID userId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            DeliveryAddressEntity entity = new DeliveryAddressEntity();
            when(repository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(entity));

            deleter.delete(userId, addressId);

            verify(repository).delete(entity);
        }

        @Test
        @DisplayName("Throws DeliveryAddressNotFoundException when address not found")
        void delete_notFound_throwsDeliveryAddressNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            when(repository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deleter.delete(userId, addressId))
                    .isInstanceOf(DeliveryAddressNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("DeliveryAddressDefaultSetter")
    class DefaultSetterTests {

        @Mock
        private DeliveryAddressRepository repository;
        @Mock
        private DeliveryAddressDtoConverter converter;
        @InjectMocks
        private DeliveryAddressDefaultSetter defaultSetter;

        @Test
        @DisplayName("Clears previous default and sets new one")
        void setDefault_existingAddress_clearsAndSetsDefault() {
            UUID userId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            DeliveryAddressEntity entity = new DeliveryAddressEntity();
            entity.setDefault(false);
            DeliveryAddressDto dto = new DeliveryAddressDto();

            when(repository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(entity));
            when(repository.save(entity)).thenReturn(entity);
            when(converter.toDto(entity)).thenReturn(dto);

            DeliveryAddressDto result = defaultSetter.setDefault(userId, addressId);

            assertThat(result).isEqualTo(dto);
            assertThat(entity.isDefault()).isTrue();
            verify(repository).clearDefaultForUser(userId);
        }

        @Test
        @DisplayName("Throws DeliveryAddressNotFoundException when address not found")
        void setDefault_notFound_throwsDeliveryAddressNotFoundException() {
            UUID userId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();
            when(repository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> defaultSetter.setDefault(userId, addressId))
                    .isInstanceOf(DeliveryAddressNotFoundException.class);
        }
    }
}

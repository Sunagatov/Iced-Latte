package com.zufar.icedlatte.user.service;

import com.zufar.icedlatte.common.exception.NotFoundException;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressRequest;
import com.zufar.icedlatte.user.converter.DeliveryAddressDtoConverter;
import com.zufar.icedlatte.user.entity.DeliveryAddressEntity;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeliveryAddressService unit tests")
class DeliveryAddressServiceTest {

    @Mock private DeliveryAddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeliveryAddressDtoConverter converter;
    @InjectMocks private DeliveryAddressService service;

    @Test
    @DisplayName("getAll returns mapped DTOs for all user addresses")
    void getAll_returnsAllAddresses() {
        UUID userId = UUID.randomUUID();
        DeliveryAddressEntity entity = new DeliveryAddressEntity();
        DeliveryAddressDto dto = new DeliveryAddressDto();
        when(addressRepository.findAllByUserId(userId)).thenReturn(List.of(entity));
        when(converter.toDto(entity)).thenReturn(dto);

        assertThat(service.getAll(userId)).containsExactly(dto);
        verify(converter).toDto(entity);
    }

    @Test
    @DisplayName("getAll returns empty list when user has no addresses")
    void getAll_noAddresses_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(addressRepository.findAllByUserId(userId)).thenReturn(List.of());

        assertThat(service.getAll(userId)).isEmpty();
    }

    @Test
    @DisplayName("create marks first address as default")
    void create_firstAddress_isSetAsDefault() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        DeliveryAddressRequest request = new DeliveryAddressRequest();
        DeliveryAddressEntity entity = new DeliveryAddressEntity();
        DeliveryAddressDto dto = new DeliveryAddressDto();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(converter.toEntity(request)).thenReturn(entity);
        when(addressRepository.existsByUserId(userId)).thenReturn(false);
        when(addressRepository.save(entity)).thenReturn(entity);
        when(converter.toDto(entity)).thenReturn(dto);

        assertThat(service.create(userId, request)).isEqualTo(dto);
        assertThat(entity.isDefault()).isTrue();
        assertThat(entity.getUser()).isSameAs(user);
        var inOrder = inOrder(userRepository, addressRepository, converter);
        inOrder.verify(userRepository).findById(userId);
        inOrder.verify(addressRepository).existsByUserId(userId);
        inOrder.verify(converter).toEntity(request);
        inOrder.verify(addressRepository).save(entity);
        inOrder.verify(converter).toDto(entity);
    }

    @Test
    @DisplayName("create does NOT mark subsequent address as default")
    void create_subsequentAddress_isNotDefault() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        DeliveryAddressRequest request = new DeliveryAddressRequest();
        DeliveryAddressEntity entity = new DeliveryAddressEntity();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(converter.toEntity(request)).thenReturn(entity);
        when(addressRepository.existsByUserId(userId)).thenReturn(true);
        when(addressRepository.save(entity)).thenReturn(entity);
        when(converter.toDto(entity)).thenReturn(new DeliveryAddressDto());

        service.create(userId, request);

        assertThat(entity.isDefault()).isFalse();
        assertThat(entity.getUser()).isSameAs(user);
        verify(addressRepository).save(entity);
        verify(converter).toDto(entity);
    }

    @Test
    @DisplayName("create retries as non-default when concurrent first-default insert wins")
    void create_concurrentDefaultConflict_retriesAsNonDefault() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        DeliveryAddressRequest request = new DeliveryAddressRequest();
        DeliveryAddressEntity entity = new DeliveryAddressEntity();
        DeliveryAddressDto dto = new DeliveryAddressDto();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(converter.toEntity(request)).thenReturn(entity);
        when(addressRepository.existsByUserId(userId)).thenReturn(false);
        when(addressRepository.save(entity))
                .thenThrow(new DataIntegrityViolationException("duplicate default"))
                .thenReturn(entity);
        when(converter.toDto(entity)).thenReturn(dto);

        assertThat(service.create(userId, request)).isEqualTo(dto);
        assertThat(entity.isDefault()).isFalse();
        verify(addressRepository, times(2)).save(entity);
    }

    @Test
    @DisplayName("create throws UserNotFoundException when user does not exist")
    void create_userNotFound_throws() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(userId, new DeliveryAddressRequest()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("update updates all fields and returns DTO")
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

        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(entity));
        when(addressRepository.save(entity)).thenReturn(entity);
        when(converter.toDto(entity)).thenReturn(dto);

        assertThat(service.update(userId, addressId, request)).isEqualTo(dto);
        assertThat(entity.getLabel()).isEqualTo("Home");
        assertThat(entity.getLine()).isEqualTo("1 Main St");
        assertThat(entity.getCity()).isEqualTo("London");
        assertThat(entity.getCountry()).isEqualTo("GB");
        assertThat(entity.getPostcode()).isEqualTo("SW1A 1AA");
    }

    @Test
    @DisplayName("update throws NotFoundException when not found")
    void update_notFound_throws() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(userId, addressId, new DeliveryAddressRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("delete removes address when found")
    void delete_existingAddress_deletesIt() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        DeliveryAddressEntity entity = new DeliveryAddressEntity();
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(entity));

        service.delete(userId, addressId);

        verify(addressRepository).delete(entity);
    }

    @Test
    @DisplayName("delete throws NotFoundException when not found")
    void delete_notFound_throws() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, addressId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("setDefault clears previous default and sets new one")
    void setDefault_existingAddress_clearsAndSetsDefault() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        DeliveryAddressEntity entity = new DeliveryAddressEntity();
        entity.setDefault(false);
        DeliveryAddressDto dto = new DeliveryAddressDto();

        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(entity));
        when(addressRepository.save(entity)).thenReturn(entity);
        when(converter.toDto(entity)).thenReturn(dto);

        assertThat(service.setDefault(userId, addressId)).isEqualTo(dto);
        assertThat(entity.isDefault()).isTrue();
        var inOrder = inOrder(addressRepository, converter);
        inOrder.verify(addressRepository).findByIdAndUserId(addressId, userId);
        inOrder.verify(addressRepository).clearDefaultForUser(userId);
        inOrder.verify(addressRepository).save(entity);
        inOrder.verify(converter).toDto(entity);
    }

    @Test
    @DisplayName("setDefault throws NotFoundException when not found")
    void setDefault_notFound_throws() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setDefault(userId, addressId))
                .isInstanceOf(NotFoundException.class);
    }
}

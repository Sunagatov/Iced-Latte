package com.zufar.icedlatte.user.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.common.exception.NotFoundException;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressRequest;
import com.zufar.icedlatte.user.api.UserAddressApi;
import com.zufar.icedlatte.user.api.UserAddressSnapshot;
import com.zufar.icedlatte.user.converter.DeliveryAddressDtoConverter;
import com.zufar.icedlatte.user.entity.DeliveryAddressEntity;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import com.zufar.icedlatte.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryAddressService implements UserAddressApi {

    private final DeliveryAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final DeliveryAddressDtoConverter converter;

    @Transactional(readOnly = true)
    public List<DeliveryAddressDto> getAll(UUID userId) {
        return addressRepository.findAllByUserId(userId).stream()
                .map(converter::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserAddressSnapshot getDeliveryAddress(UUID userId, UUID deliveryAddressId) {
        var entity = addressRepository
                .findByIdAndUserId(deliveryAddressId, userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Delivery address with id = %s is not found.", deliveryAddressId)));
        return new UserAddressSnapshot(entity.getCountry(), entity.getCity(), entity.getLine(), entity.getPostcode());
    }

    @Transactional
    public DeliveryAddressDto create(UUID userId, DeliveryAddressRequest request) {
        var user = userRepository.findByIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));
        boolean shouldBecomeDefault = !addressRepository.existsByUserId(userId);
        var entity = converter.toEntity(request);
        entity.setUser(user);
        entity.setDefault(shouldBecomeDefault);
        return converter.toDto(addressRepository.save(entity));
    }

    @Transactional
    public DeliveryAddressDto update(UUID userId, UUID addressId, DeliveryAddressRequest request) {
        var entity = addressRepository
                .findByIdAndUserId(addressId, userId)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Delivery address with id = %s is not found.", addressId)));
        entity.setLabel(request.getLabel());
        entity.setLine(request.getLine());
        entity.setCity(request.getCity());
        entity.setCountry(request.getCountry());
        entity.setPostcode(request.getPostcode());
        return converter.toDto(addressRepository.save(entity));
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        var entity = addressRepository
                .findByIdAndUserId(addressId, userId)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Delivery address with id = %s is not found.", addressId)));
        var replacement = entity.isDefault()
                ? addressRepository.findFirstByUserIdAndIdNotOrderByIdAsc(userId, addressId)
                : Optional.<DeliveryAddressEntity>empty();
        addressRepository.delete(entity);
        replacement.ifPresent(address -> {
            addressRepository.flush();
            address.setDefault(true);
            addressRepository.save(address);
        });
    }

    @Transactional
    public DeliveryAddressDto setDefault(UUID userId, UUID addressId) {
        var entity = addressRepository
                .findByIdAndUserId(addressId, userId)
                .orElseThrow(() ->
                        new NotFoundException(String.format("Delivery address with id = %s is not found.", addressId)));
        if (entity.isDefault()) {
            return converter.toDto(entity);
        }
        addressRepository.clearDefaultForUser(userId);
        entity.setDefault(true);
        return converter.toDto(addressRepository.save(entity));
    }
}

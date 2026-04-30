package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressRequest;
import com.zufar.icedlatte.user.converter.DeliveryAddressDtoConverter;
import com.zufar.icedlatte.user.entity.DeliveryAddressEntity;
import com.zufar.icedlatte.user.exception.DeliveryAddressNotFoundException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryAddressService {

    private final DeliveryAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final DeliveryAddressDtoConverter converter;

    @Transactional(readOnly = true)
    public List<DeliveryAddressDto> getAll(UUID userId) {
        return addressRepository.findAllByUserId(userId).stream()
                .map(converter::toDto)
                .toList();
    }

    @Transactional
    public DeliveryAddressDto create(UUID userId, DeliveryAddressRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        boolean shouldBecomeDefault = !addressRepository.existsByUserId(userId);
        var entity = converter.toEntity(request);
        entity.setUser(user);
        entity.setDefault(shouldBecomeDefault);
        return converter.toDto(saveAddress(entity));
    }

    @Transactional
    public DeliveryAddressDto update(UUID userId, UUID addressId, DeliveryAddressRequest request) {
        var entity = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new DeliveryAddressNotFoundException(addressId));
        entity.setLabel(request.getLabel());
        entity.setLine(request.getLine());
        entity.setCity(request.getCity());
        entity.setCountry(request.getCountry());
        entity.setPostcode(request.getPostcode());
        return converter.toDto(addressRepository.save(entity));
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        var entity = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new DeliveryAddressNotFoundException(addressId));
        addressRepository.delete(entity);
    }

    @Transactional
    public DeliveryAddressDto setDefault(UUID userId, UUID addressId) {
        var entity = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new DeliveryAddressNotFoundException(addressId));
        addressRepository.clearDefaultForUser(userId);
        entity.setDefault(true);
        return converter.toDto(addressRepository.save(entity));
    }

    private DeliveryAddressEntity saveAddress(DeliveryAddressEntity entity) {
        try {
            return addressRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            if (!entity.isDefault()) {
                throw ex;
            }
            // Another request created the first default address concurrently.
            entity.setDefault(false);
            return addressRepository.save(entity);
        }
    }
}

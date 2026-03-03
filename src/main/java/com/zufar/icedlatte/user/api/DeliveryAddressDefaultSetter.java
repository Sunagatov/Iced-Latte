package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.user.converter.DeliveryAddressDtoConverter;
import com.zufar.icedlatte.user.exception.DeliveryAddressNotFoundException;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryAddressDefaultSetter {

    private final DeliveryAddressRepository repository;
    private final DeliveryAddressDtoConverter converter;

    @Transactional
    public DeliveryAddressDto setDefault(UUID userId, UUID addressId) {
        var entity = repository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new DeliveryAddressNotFoundException(addressId));
        repository.clearDefaultForUser(userId);
        entity.setDefault(true);
        return converter.toDto(repository.save(entity));
    }
}

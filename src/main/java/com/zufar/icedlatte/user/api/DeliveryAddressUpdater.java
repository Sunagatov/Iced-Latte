package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressRequest;
import com.zufar.icedlatte.user.converter.DeliveryAddressDtoConverter;
import com.zufar.icedlatte.user.exception.DeliveryAddressNotFoundException;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryAddressUpdater {

    private final DeliveryAddressRepository repository;
    private final DeliveryAddressDtoConverter converter;

    @Transactional
    public DeliveryAddressDto update(UUID userId, UUID addressId, DeliveryAddressRequest request) {
        var entity = repository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new DeliveryAddressNotFoundException(addressId));
        entity.setLabel(request.getLabel());
        entity.setLine(request.getLine());
        entity.setCity(request.getCity());
        entity.setCountry(request.getCountry());
        entity.setPostcode(request.getPostcode());
        return converter.toDto(repository.save(entity));
    }
}

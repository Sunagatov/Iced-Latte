package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.user.converter.DeliveryAddressDtoConverter;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryAddressProvider {

    private final DeliveryAddressRepository repository;
    private final DeliveryAddressDtoConverter converter;

    @Transactional(readOnly = true)
    public List<DeliveryAddressDto> getAll(UUID userId) {
        return repository.findAllByUserId(userId).stream()
                .map(converter::toDto)
                .toList();
    }
}

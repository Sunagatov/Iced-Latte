package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.user.exception.DeliveryAddressNotFoundException;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryAddressDeleter {

    private final DeliveryAddressRepository repository;

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        var entity = repository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new DeliveryAddressNotFoundException(addressId));
        repository.delete(entity);
    }
}

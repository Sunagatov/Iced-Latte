package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.DeliveryAddressDto;
import com.zufar.icedlatte.openapi.dto.DeliveryAddressRequest;
import com.zufar.icedlatte.user.converter.DeliveryAddressDtoConverter;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import com.zufar.icedlatte.user.repository.DeliveryAddressRepository;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryAddressCreator {

    private final DeliveryAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final DeliveryAddressDtoConverter converter;

    @Transactional
    public DeliveryAddressDto create(UUID userId, DeliveryAddressRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        var entity = converter.toEntity(request);
        entity.setUser(user);
        boolean isFirst = addressRepository.findAllByUserId(userId).isEmpty();
        entity.setDefault(isFirst);
        return converter.toDto(addressRepository.save(entity));
    }
}

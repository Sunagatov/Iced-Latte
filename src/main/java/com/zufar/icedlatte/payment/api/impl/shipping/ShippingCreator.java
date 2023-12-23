package com.zufar.icedlatte.payment.api.impl.shipping;

import com.zufar.icedlatte.openapi.dto.ShippingInfoDto;
import com.zufar.icedlatte.payment.converter.ShippingConverter;
import com.zufar.icedlatte.payment.entity.Shipping;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.payment.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class ShippingCreator {

    private final ShippingRepository shippingRepository;

    private final SingleUserProvider singleUserProvider;

    private final SecurityPrincipalProvider securityUserProvider;

    private final ShippingConverter shippingConverter;

    @Transactional
    public ShippingInfoDto createShipping(ShippingInfoDto shippingInfoDto) {
        UUID userId = securityUserProvider.getUserId();
        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        Shipping convertedEntity = shippingConverter.toEntity(shippingInfoDto);
        userEntity.addShipping(convertedEntity);
        Shipping savedEntity = shippingRepository.save(convertedEntity);
        return shippingConverter.toDto(savedEntity);
    }
}

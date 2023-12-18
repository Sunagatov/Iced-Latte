package com.zufar.icedlatte.payment.api.impl.shipping;

import com.zufar.icedlatte.openapi.dto.ShippingInfoDto;
import com.zufar.icedlatte.payment.converter.ShippingConverter;
import com.zufar.icedlatte.payment.entity.Shipping;
import com.zufar.icedlatte.user.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Log4j2
public class ShippingCreator {

    private final ShippingRepository shippingRepository;

    private final ShippingConverter shippingConverter;

    @Transactional
    public ShippingInfoDto createShipping(ShippingInfoDto shippingInfoDto) {
        Shipping convertedEntity = shippingConverter.toEntity(shippingInfoDto);
        Shipping savedEntity = shippingRepository.save(convertedEntity);
        return shippingConverter.toDto(savedEntity);
    }
}

package com.zufar.icedlatte.payment.api.impl.shipping;

import com.zufar.icedlatte.openapi.dto.ShippingInfoDto;
import com.zufar.icedlatte.payment.converter.ShippingConverter;
import com.zufar.icedlatte.payment.entity.Shipping;
import com.zufar.icedlatte.payment.exception.ShippingDoesNotExistException;
import com.zufar.icedlatte.payment.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class ShippingRetriever {

    private final ShippingRepository shippingRepository;

    private final ShippingConverter shippingConverter;

    @Transactional(readOnly = true)
    public ShippingInfoDto getShippingById(final Long shippingId) {
        Shipping currentShipping = shippingRepository.findByShippingId(shippingId);
        if (currentShipping == null) {
            log.error("The Shipping with id = {} is not found.", shippingId);
            throw new ShippingDoesNotExistException(shippingId);
        }
        return shippingConverter.toDto(currentShipping);
    }

    @Transactional(readOnly = true)
    public List<ShippingInfoDto> getDeliveriesByUserId(final UUID userId) {
        List<Shipping> deliveries = shippingRepository.findDeliveriesByUserId(userId);
        if (deliveries == null || deliveries.isEmpty()) {
            log.warn("No deliveries found for user with id = {} ", userId);
            return Collections.emptyList();
        }
        return shippingConverter.toDtoList(deliveries);
    }
}

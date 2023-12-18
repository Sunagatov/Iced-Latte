package com.zufar.icedlatte.payment.api.impl;

import com.zufar.icedlatte.openapi.dto.ShippingInfoDto;
import com.zufar.icedlatte.payment.api.ShippingApi;
import com.zufar.icedlatte.payment.api.impl.shipping.ShippingRetriever;
import com.zufar.icedlatte.payment.api.impl.shipping.ShippingCreator;
import com.zufar.icedlatte.payment.api.impl.shipping.ShippingDeleter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class ShippingApiImpl implements ShippingApi {

    private final ShippingRetriever shippingRetriever;

    private final ShippingCreator shippingCreator;

    private final ShippingDeleter shippingDeleter;

    @Override
    public ShippingInfoDto getById(Long shippingId) {
        return shippingRetriever.getShippingById(shippingId);
    }

    @Override
    public ShippingInfoDto create(ShippingInfoDto shippingInfoDto) {
        return shippingCreator.createShipping(shippingInfoDto);
    }

    @Override
    public void deleteById(Long shippingId) {
        shippingDeleter.deleteShippingById(shippingId);
    }

    @Override
    public List<ShippingInfoDto> getAllByUserId(UUID userId) {
        return shippingRetriever.getDeliveriesByUserId(userId);
    }
}

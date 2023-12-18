package com.zufar.icedlatte.payment.api;

import com.zufar.icedlatte.openapi.dto.ShippingInfoDto;

import java.util.List;
import java.util.UUID;

public interface ShippingApi {

    ShippingInfoDto getById(Long shippingId);

    ShippingInfoDto create(ShippingInfoDto shippingInfoDto);

    void deleteById(Long shippingId);

    List<ShippingInfoDto> getAllByUserId(UUID userId);

}

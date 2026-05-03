package com.zufar.icedlatte.order.validator;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.CreateNewOrderRequestDto;
import org.springframework.stereotype.Component;

@Component
public class OrderAddressValidator {

    public void validate(CreateNewOrderRequestDto request) {
        boolean hasAddressId = request.getDeliveryAddressId() != null;
        boolean hasInlineAddress = request.getAddress() != null;

        if (!hasAddressId && !hasInlineAddress) {
            throw new BadRequestException("Either 'deliveryAddressId' or 'address' must be provided.");
        }
        if (hasAddressId && hasInlineAddress) {
            throw new BadRequestException("Provide either 'deliveryAddressId' or 'address', not both.");
        }
    }
}

package com.zufar.onlinestore.payment.converter;

import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.entity.Payment;
import org.mapstruct.Mapper;

@Mapper
public interface PaymentConverter {

    PaymentDetailsDto toDto(final Payment entity);

    Payment toEntity(final PaymentDetailsDto dto);
}

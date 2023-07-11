package com.zufar.onlinestore.notification.converter;

import com.zufar.onlinestore.customer.converter.CustomerDtoConverter;
import com.zufar.onlinestore.customer.dto.CustomerDto;
import com.zufar.onlinestore.notification.dto.NotificationDto;
import com.zufar.onlinestore.notification.entity.Notification;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Alex Zarubin
 * created on 27.05.2023
 */
@Service
@AllArgsConstructor
public class NotificationDtoConverter {
    private final CustomerDtoConverter customerDtoConverter;

    public NotificationDto convertToDto(final Notification entity) {
        CustomerDto customerDto = customerDtoConverter.convertToDto(entity.getRecipient());
        return NotificationDto.builder()
                .message(entity.getMessage())
                .recipient(customerDto)
                .build();
    }

    public Notification convertToEntity(final NotificationDto dto) {
        CustomerDto customerDto = dto.getRecipient();
        return Notification.builder()
                .message(dto.getMessage())
                .recipient(customerDtoConverter.convertToEntity(customerDto))
                .build();
    }
}

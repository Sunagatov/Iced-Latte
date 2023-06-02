package com.zufar.onlinestore.notification.dto;

import com.zufar.onlinestore.customer.dto.CustomerDto;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Alex Zarubin
 * created on 27.05.2023
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    @Nullable
    private int id;

    @NotBlank(message = "Message is mandatory")
    @Size(max = 255, message = "Message length must be less than 255 characters")
    private String message;

    private CustomerDto recipient;
}

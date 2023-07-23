package com.zufar.onlinestore.reservation.dto;

import com.zufar.onlinestore.reservation.entity.ReservationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.Instant;

public record ReservationDto(
        @NotNull(message = "ReservationId is the mandatory attribute")
        Integer reservationId,

        @NotBlank(message = "ProductId is the mandatory attribute")
        String productId,

        @NotNull(message = "ReservedQuantity is the mandatory attribute")
        Integer reservedQuantity,

        @NotNull(message = "CreatedAt is the mandatory attribute")
        @Past
        Instant createdAt,

        @NotBlank(message = "Status is the mandatory attribute")
        ReservationStatus status
) {
}
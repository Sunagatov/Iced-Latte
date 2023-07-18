package com.zufar.onlinestore.reservation.dto;


import java.time.LocalDateTime;

public record ReservationDto(
        int reservationId,
        int productId,
        int reservedQuantity,
        LocalDateTime createdAt,
        String reservationStatus
) {}
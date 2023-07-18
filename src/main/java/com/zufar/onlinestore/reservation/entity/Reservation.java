package com.zufar.onlinestore.reservation.entity;

import lombok.Builder;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Builder
public record Reservation(
    @Id
    int reservationId,
    int productId,
    int reservedQuantity,
    LocalDateTime createdAt,
    String reservationStatus
) {}


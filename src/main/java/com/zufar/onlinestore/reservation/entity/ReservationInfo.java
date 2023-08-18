package com.zufar.onlinestore.reservation.entity;

import java.time.Instant;
import java.util.UUID;

public record ReservationInfo(UUID reservationId, Instant createdAt) {
}
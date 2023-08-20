package com.zufar.onlinestore.reservation.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Every user has only one 'active reservation' and this reservation in status = {@link ReservationStatus#CREATED}
 * In other words 'active reservation' means current created reservation in status {@link ReservationStatus#CREATED}
 */
public record ActiveReservation(UUID reservationId, Instant createdAt, Instant expiredAt) {
}
package com.zufar.onlinestore.reservation.exception;

import java.util.UUID;

public class ConcurrentReservationRollbackException extends RuntimeException {

    public ConcurrentReservationRollbackException(UUID userId) {
        super("Reservation has been aborted due to concurrent reservation for user = %s".formatted(userId));
    }
}

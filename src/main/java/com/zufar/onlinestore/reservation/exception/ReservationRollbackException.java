package com.zufar.onlinestore.reservation.exception;

import java.util.UUID;

public class ReservationRollbackException extends RuntimeException {

    public ReservationRollbackException(UUID userId) {
        super("Reservation has been aborted due to concurrent reservation for user = %s".formatted(userId));
    }
}

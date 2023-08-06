package com.zufar.onlinestore.reservation.exception;

import java.util.UUID;

public class ReservationAbortedException extends RuntimeException {

    public ReservationAbortedException(final UUID reservationId) {
        super(String.format("Reservation has been aborted due to parallel insertion with the same reservationId = %s", reservationId));
    }
}

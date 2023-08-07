package com.zufar.onlinestore.reservation.api.dto.confirmation;

/**
 * @param status Status of confirmation
 */
public record ConfirmedReservationResponse(

        ConfirmedReservationStatus status
) {
}

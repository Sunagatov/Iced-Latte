package com.zufar.onlinestore.reservation.api.dto.cancellation;

/**
 * @param status Status of cancellation
 */
public record CancelledReservationResponse(

        CancelledReservationStatus status
) {
}

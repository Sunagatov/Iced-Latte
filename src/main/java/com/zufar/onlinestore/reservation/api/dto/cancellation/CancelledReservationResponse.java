package com.zufar.onlinestore.reservation.api.dto.cancellation;

/**
 * @param status Status of cancellation
 */
public record CancelledReservationResponse(
        CancelledReservationStatus status
) {
    public static CancelledReservationResponse cancelled() {
        return new CancelledReservationResponse(CancelledReservationStatus.CANCELLED);
    }

    public static CancelledReservationResponse notCancelled() {
        return new CancelledReservationResponse(CancelledReservationStatus.NOT_CANCELLED);
    }
}

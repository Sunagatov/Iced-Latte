package com.zufar.onlinestore.reservation.api.dto.confirmation;

/**
 * @param status Status of confirmation
 */
public record ConfirmedReservationResponse(

        ConfirmedReservationStatus status
) {

    public static ConfirmedReservationResponse confirmed() {
        return new ConfirmedReservationResponse(ConfirmedReservationStatus.CONFIRMED);
    }

    public static ConfirmedReservationResponse notConfirmed() {
        return new ConfirmedReservationResponse(ConfirmedReservationStatus.NOT_CONFIRMED);
    }

}

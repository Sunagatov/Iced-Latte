package com.zufar.onlinestore.reservation.api.dto.confirmation;

import jakarta.validation.constraints.NotNull;

/**
 * @param status Status of confirmation
 */
public record ConfirmedReservationResponse(

        @NotNull
        ConfirmedReservationStatus status
) {
}

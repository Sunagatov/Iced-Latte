package com.zufar.onlinestore.reservation.api.dto.cancellation;

import jakarta.validation.constraints.NotNull;

/**
 * @param status Status of cancellation
 */
public record CancelledReservationResponse(

        @NotNull
        CancelledReservationStatus status
) {
}

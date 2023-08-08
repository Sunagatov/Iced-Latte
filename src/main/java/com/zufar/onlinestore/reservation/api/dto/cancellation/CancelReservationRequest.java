package com.zufar.onlinestore.reservation.api.dto.cancellation;

import java.util.UUID;

/**
 * @param reservationId ID of reservation for cancellation
 */

public record CancelReservationRequest(

        UUID reservationId
) {
}

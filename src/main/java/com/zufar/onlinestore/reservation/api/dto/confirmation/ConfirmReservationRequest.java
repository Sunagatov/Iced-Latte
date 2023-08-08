package com.zufar.onlinestore.reservation.api.dto.confirmation;

import java.util.UUID;

/**
 * @param reservationId ID of reservation for confirmation
 */

public record ConfirmReservationRequest(

        UUID reservationId
) {
}

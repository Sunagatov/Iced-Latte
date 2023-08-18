package com.zufar.onlinestore.reservation.api.dto.creation;

import java.util.List;
import java.util.UUID;

/**
 * @param userId       ID of user who want to make reservation
 * @param reservations The list of products for reservation
 */

public record CreateReservationRequest(

        UUID userId,

        List<ProductReservation> reservations
) {
}

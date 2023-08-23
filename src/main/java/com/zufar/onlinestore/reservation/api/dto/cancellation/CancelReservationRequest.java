package com.zufar.onlinestore.reservation.api.dto.cancellation;

import java.util.UUID;

/**
 * @param userId ID of user who wants to cancel created reservation
 */
public record CancelReservationRequest(

        UUID userId
) {
}

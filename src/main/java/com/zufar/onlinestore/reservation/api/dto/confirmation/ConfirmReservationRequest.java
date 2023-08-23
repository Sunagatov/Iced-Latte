package com.zufar.onlinestore.reservation.api.dto.confirmation;

import java.util.UUID;

/**
 * @param userId ID of user who wants to confirm created reservation
 */
public record ConfirmReservationRequest(

        UUID userId
) {
}

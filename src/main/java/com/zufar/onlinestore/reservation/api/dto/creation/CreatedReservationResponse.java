package com.zufar.onlinestore.reservation.api.dto.creation;

import java.time.Instant;
import java.util.List;

import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationStatus.NOTHING_RESERVED;
import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationStatus.RESERVED;
import static java.util.Collections.emptyList;

/**
 * @param status       Status of reservation
 * @param reservations The list of products that are reserved
 *                     May be empty in case of {@link CreatedReservationStatus#NOTHING_RESERVED}
 * @param expiredAt    The time moment in UTC when the created reservation will be expired
 *                     May be NULL in case of {@link CreatedReservationStatus#NOTHING_RESERVED}
 */
public record CreatedReservationResponse(

        CreatedReservationStatus status,

        List<ProductReservation> reservations,

        Instant expiredAt
) {

    public static CreatedReservationResponse reserved(final List<ProductReservation> reservations, final Instant expiredAt) {
        return new CreatedReservationResponse(RESERVED, reservations, expiredAt);
    }

    public static CreatedReservationResponse nothingReserved() {
        return new CreatedReservationResponse(NOTHING_RESERVED, emptyList(), null);
    }
}
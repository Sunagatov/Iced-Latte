package com.zufar.onlinestore.reservation.api.dto.creation;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationStatus.FAILED_RESERVATION;
import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationStatus.SUCCESSFUL_RESERVATION;
import static java.util.Collections.emptyList;

/**
 * @param status       Status of reservation
 * @param reservations The list of products that are reserved
 *                     May be empty in case of {@link CreatedReservationStatus#FAILED_RESERVATION}
 * @param expiredAt    The time moment in UTC when the created reservation will be expired
 *                     May be NULL in case of {@link CreatedReservationStatus#FAILED_RESERVATION}
 */
public record CreatedReservationResponse(

        @NotNull
        CreatedReservationStatus status,

        @NotNull
        List<ProductReservation> reservations,

        @Nullable
        Instant expiredAt
) {

    public static CreatedReservationResponse successfulReservation(final List<ProductReservation> reservations, final Instant expiredAt) {
        return new CreatedReservationResponse(SUCCESSFUL_RESERVATION, reservations, expiredAt);
    }

    public static CreatedReservationResponse failedReservation() {
        return new CreatedReservationResponse(FAILED_RESERVATION, emptyList(), null);
    }
}
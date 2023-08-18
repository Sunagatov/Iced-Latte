package com.zufar.onlinestore.reservation.api.dto.creation;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @param reservations The list of products for reservation
 */
public record CreateReservationDto(

        @NotEmpty
        List<ProductReservation> reservations
) {
}

package com.zufar.onlinestore.reservation.api.dto.creation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * @param reservationId       Bind products together in one reservation
 * @param productReservations The list of products for reservation
 */

public record CreateReservationRequest(

        /*
         *  TODO get current logged-in user and bind reservationId to him in Endpoint(Controller) layer:
         *   UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
         *   if(hasActiveReservation(user)){ // active means that reservation is not completed and in status CREATED
         *     reservationId = getActiveReservationId()
         *   } else {
         *     var reservationId = UUID.randomUUID()
         *     reservationId = bindNewReservationToUser(reservationId, user)
         *   }
         * */
        @NotNull
        UUID reservationId,

        @NotEmpty
        List<ProductReservation> productReservations
) {
}

package com.zufar.onlinestore.reservation.api;

import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.cancellation.CancelledReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.confirmation.ConfirmedReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse;

/**
 * API to maintain reservation's lifecycle
 */
public interface ReservationApi {

    /**
     * Method to create a reservation for a certain quantity of products (before payment)
     * Reservations can be made partially
     * This method is idempotent and match behaviour of HTTP.PUT method
     * If you send the same data twice or more times then you will have the same state
     * </p>
     * Example or reservation,
     * You want to reserve next items to buy [Nokia = 3, iPhone = 2, Xiaomi = 4, Samsung = 1]
     * But stock only has [Nokia = 2, iPhone = 3, Xiaomi = 0]
     * There are 4 cases:
     * 1. [iPhone]  will be FULLY RESERVED because it is enough in stock
     * 2. [Nokia]   will be PARTIALLY RESERVED because we have only 2 of 3 items in stock
     * 3. [Samsung] will not be reserved because it is NOT EXIST
     * 4. [Xiaomi]  will not be reserved because it is OUT OF STOCK
     *
     * @param createReservation request to create the reservation
     * @return the status of the created reservation
     */
    CreatedReservationResponse createReservation(final CreateReservationRequest createReservation);

    /**
     * Method to confirm the reservation (after payment)
     *
     * @return the status of the confirmed reservation
     */
    ConfirmedReservationResponse confirmReservation();


    /**
     * Method to cancel the reservation
     *
     * @return the status of the cancelled reservation
     */
    CancelledReservationResponse cancelReservation();
}

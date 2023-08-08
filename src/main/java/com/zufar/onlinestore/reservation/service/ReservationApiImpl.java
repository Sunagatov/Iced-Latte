package com.zufar.onlinestore.reservation.service;

import com.zufar.onlinestore.reservation.api.ReservationApi;
import com.zufar.onlinestore.reservation.api.dto.cancellation.CancelReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.cancellation.CancelledReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.confirmation.ConfirmReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.confirmation.ConfirmedReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse.failedReservation;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReservationApiImpl implements ReservationApi {

    private final ReservationCreator reservationCreator;

    @Override
    public CreatedReservationResponse createReservation(final CreateReservationRequest request) {
        try {
            return reservationCreator.tryToCreateReservation(request);
        } catch (RuntimeException rollbackException) {
            return failedReservation();
        }
    }

    @Override
    public ConfirmedReservationResponse confirmReservation(final ConfirmReservationRequest confirmReservation) {
        // TODO:
        //  UPDATE reservation SET status = 'CONFIRMED' where status = 'CREATED' and reservation_id = ?
        //  ---
        //  we can confirm reservation only from status CREATED
        //  CONFIRMED is a final status and you should not change it to any other status
        return null;
    }

    @Transactional
    @Override
    public CancelledReservationResponse cancelReservation(final CancelReservationRequest cancelReservation) {
        // TODO:
        //  WITH cancelled_reservations AS (
        //   UPDATE reservation r SET status = 'CANCELLED' where reservation_id = ? AND status = 'CREATED'
        //   RETURNING r.product_id, r.reserved_quantity
        //  )
        //  UPDATE product p SET quantity = p.quantity + cancelled_reservations.reserved_quantity
        //  WHERE id = cancelled_reservations.product_id
        //  ---
        //  we can cancel reservation only from status CREATED
        //  CANCELLED is a final status and you should not change it to any other status
        return null;
    }
}



package com.zufar.onlinestore.reservation.service;

import com.zufar.onlinestore.reservation.api.ReservationApi;
import com.zufar.onlinestore.reservation.api.dto.cancellation.CancelledReservationResponse;
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
    public ConfirmedReservationResponse confirmReservation() {
        // TODO: UPDATE reservation SET status = 'CONFIRMED' where status = 'CREATED' and reservation_id = ?
        //  we can confirm reservation only from status CREATED
        //  CONFIRMED is a final status and you should not change it to any other status
        return null;
    }

    @Transactional
    @Override
    public CancelledReservationResponse cancelReservation() {
        // TODO: UPDATE reservation SET status = 'CANCELLED' status = 'CREATED' where reservation_id = ?
        //  we can cancel reservation only from status CREATED
        //  CANCELLED is a final status and you should not change it to any other status
        return null;
    }
}



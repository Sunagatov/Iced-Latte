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

import static com.zufar.onlinestore.reservation.api.dto.cancellation.CancelledReservationResponse.notCancelled;
import static com.zufar.onlinestore.reservation.api.dto.confirmation.ConfirmedReservationResponse.notConfirmed;
import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse.nothingReserved;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReservationApiImpl implements ReservationApi {

    private final ReservationCreator reservationCreator;
    private final ReservationConfirmer reservationConfirmer;
    private final ReservationCanceller reservationCanceller;

    @Override
    public CreatedReservationResponse createReservation(final CreateReservationRequest request) {
        try {
            return reservationCreator.createReservation(request);
        } catch (RuntimeException rollbackException) {
            return nothingReserved();
        }
    }

    @Override
    public ConfirmedReservationResponse confirmReservation(final ConfirmReservationRequest request) {
        try {
            return reservationConfirmer.confirmReservation(request);
        } catch (RuntimeException rollbackException) {
            return notConfirmed();
        }
    }

    @Transactional
    @Override
    public CancelledReservationResponse cancelReservation(final CancelReservationRequest request) {
        try {
            return reservationCanceller.cancelReservation(request);
        } catch (RuntimeException rollbackException) {
            return notCancelled();
        }
    }
}



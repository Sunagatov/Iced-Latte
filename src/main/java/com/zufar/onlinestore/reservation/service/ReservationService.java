package com.zufar.onlinestore.reservation.service;

import com.zufar.onlinestore.reservation.api.ReservationApi;
import com.zufar.onlinestore.reservation.api.dto.cancellation.CancelledReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.confirmation.ConfirmedReservationResponse;
import com.zufar.onlinestore.reservation.api.dto.creation.CreateReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse;
import com.zufar.onlinestore.reservation.exception.ReservationAbortedException;
import com.zufar.onlinestore.reservation.validator.IncomingDtoValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.zufar.onlinestore.reservation.api.dto.creation.CreatedReservationResponse.failedReservation;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReservationService implements ReservationApi {

    private final IncomingDtoValidator<CreateReservationRequest> validator;
    private final ReservationCreator reservationCreator;

    @Override
    public CreatedReservationResponse createReservation(final CreateReservationRequest request) {
        var valid = validator.isValid(request);
        if (!valid) {
            return failedReservation();
        }

        try {
            return reservationCreator.tryToCreateReservation(request);
        } catch (ReservationAbortedException abortedException) {
            return failedReservation();
        }
    }

    @Override
    public ConfirmedReservationResponse confirmReservation() {
        // UPDATE reservation SET status = CONFIRMED where reservation_id = ?
        return null;
    }

    @Transactional
    @Override
    public CancelledReservationResponse cancelReservation() {
        // UPDATE reservation SET status = CANCELLED where reservation_id = ?
        return null;
    }
}



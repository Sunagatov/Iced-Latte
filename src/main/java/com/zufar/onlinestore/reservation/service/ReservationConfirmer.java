package com.zufar.onlinestore.reservation.service;

import com.zufar.onlinestore.reservation.api.dto.confirmation.ConfirmReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.confirmation.ConfirmedReservationResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import static com.zufar.onlinestore.reservation.api.dto.confirmation.ConfirmedReservationResponse.notConfirmed;
import static com.zufar.onlinestore.reservation.api.dto.confirmation.ConfirmedReservationResponse.confirmed;

@RequiredArgsConstructor
@Service
public class ReservationConfirmer {

    private static final int EXACTLY_ONE_UPDATED_ROW = 1;

    private static final String CONFIRM_RESERVATION_SQL = """
            UPDATE user_reservation_history
            SET status = 'CONFIRMED'
            WHERE user_id = :user_id AND status = 'CREATED'
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ConfirmedReservationResponse confirmReservation(ConfirmReservationRequest request) {
        var sqlParams = Map.of("user_id", request.userId());
        int updatedRowsCount = jdbcTemplate.update(CONFIRM_RESERVATION_SQL, sqlParams);
        if (updatedRowsCount == EXACTLY_ONE_UPDATED_ROW) {
            return confirmed();
        }
        return notConfirmed();
    }
}

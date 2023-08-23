package com.zufar.onlinestore.reservation.service;

import com.zufar.onlinestore.reservation.api.dto.cancellation.CancelReservationRequest;
import com.zufar.onlinestore.reservation.api.dto.cancellation.CancelledReservationResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import static com.zufar.onlinestore.reservation.api.dto.cancellation.CancelledReservationResponse.cancelled;

@RequiredArgsConstructor
@Service
public class ReservationCanceller {

    private static final String CANCEL_RESERVATION_SQL = """
            WITH cancelled_reservation AS (
             UPDATE user_reservation_history
             SET status = 'CANCELLED'
             WHERE user_id = :user_id AND status = 'CREATED'
             RETURNING reservation_id
            ), reservations AS (
             SELECT warehouse_item_id, reserved_quantity
             FROM reservation
             WHERE reservation_id = (SELECT reservation_id FROM cancelled_reservation)
            )
            UPDATE warehouse SET quantity = quantity + reservations.reserved_quantity
            FROM reservations
            WHERE item_id = reservations.warehouse_item_id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CancelledReservationResponse cancelReservation(CancelReservationRequest request) {
        var sqlParams = Map.of("user_id", request.userId());
        jdbcTemplate.update(CANCEL_RESERVATION_SQL, sqlParams);
        return cancelled();
    }
}

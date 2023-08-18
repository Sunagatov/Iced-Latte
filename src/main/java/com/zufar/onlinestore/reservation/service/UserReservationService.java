package com.zufar.onlinestore.reservation.service;

import com.zufar.onlinestore.reservation.entity.ReservationInfo;
import com.zufar.onlinestore.reservation.exception.ConcurrentReservationRollbackException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserReservationService {

    private static final String RESERVATION_ID = "reservation_id";
    private static final String CREATED_AT = "created_at";

    private static final String SELECT_RESERVATION_SQL = """
            SELECT reservation_id, created_at
            FROM user_reservation_history
            WHERE user_id = :user_id AND status = 'CREATED'
            FOR UPDATE;
            """;

    private static final String INSERT_RESERVATION_SQL = """
            INSERT INTO user_reservation_history (user_id)
            VALUES (:user_id)
            ON CONFLICT DO NOTHING;
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public ReservationInfo getCurrentReservationForUpdate(UUID userId) {
        var sqlParams = Map.of("user_id", userId);
        var currentReservation = jdbcTemplate.queryForMap(SELECT_RESERVATION_SQL, sqlParams);
        if (currentReservation.isEmpty()) {
            jdbcTemplate.update(INSERT_RESERVATION_SQL, sqlParams);
            currentReservation = jdbcTemplate.queryForMap(SELECT_RESERVATION_SQL, sqlParams);
        }
        return new ReservationInfo(
                (UUID) currentReservation.get(RESERVATION_ID),
                ((Timestamp) currentReservation.get(CREATED_AT)).toInstant()
        );
    }
}

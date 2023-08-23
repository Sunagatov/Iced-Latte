package com.zufar.onlinestore.reservation.service;

import com.zufar.onlinestore.reservation.config.ReservationTimeoutConfiguration;
import com.zufar.onlinestore.reservation.entity.ActiveReservation;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserReservationHistoryService {

    private static final String RESERVATION_ID = "reservation_id";
    private static final String CREATED_AT = "created_at";

    private static final String GET_ACTIVE_RESERVATION = """
            SELECT reservation_id, created_at
            FROM user_reservation_history
            WHERE user_id = :user_id AND status = 'CREATED'
            FOR UPDATE;
            """;

    private static final String CREATE_ACTIVE_RESERVATION_IF_NOT_EXISTS = """
            INSERT INTO user_reservation_history (user_id)
            VALUES (:user_id)
            ON CONFLICT DO NOTHING;
            """;

    private final ReservationTimeoutConfiguration timeoutConfiguration;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public @NotNull ActiveReservation getActiveReservationForUpdate(UUID userId) {
        var sqlParams = Map.of("user_id", userId);
        jdbcTemplate.update(CREATE_ACTIVE_RESERVATION_IF_NOT_EXISTS, sqlParams);
        var activeReservation = jdbcTemplate.queryForMap(GET_ACTIVE_RESERVATION, sqlParams);

        var reservationId = (UUID) activeReservation.get(RESERVATION_ID);
        var createdAt = ((Timestamp) activeReservation.get(CREATED_AT)).toInstant();
        var expiredAt = timeoutConfiguration.getExpiredAt(createdAt);

        return new ActiveReservation(reservationId, createdAt, expiredAt);
    }
}

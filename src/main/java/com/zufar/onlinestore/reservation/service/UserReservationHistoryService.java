package com.zufar.onlinestore.reservation.service;

import com.zufar.onlinestore.reservation.entity.ReservationInfo;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserReservationHistoryService {

    private static final String RESERVATION_ID = "reservation_id";
    private static final String CREATED_AT = "created_at";

    private static final String SELECT_RESERVATION_FOR_UPDATE = """
            SELECT reservation_id, created_at
            FROM user_reservation_history
            WHERE user_id = ? AND status = 'CREATED'
            FOR UPDATE;
            """;

    private static final String INSERT_RESERVATION = """
            INSERT INTO user_reservation_history (reservation_id, user_id, status, created_at)
            VALUES (?, ?, 'CREATED', ?)
            ON CONFLICT DO NOTHING;
            """;

    private static final String UPDATE_RESERVATION_IF_EXISTS = """
            UPDATE user_reservation_history
            SET created_at = ?
            WHERE user_id = ? AND status = 'CREATED'
            """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void createReservationIfNotExistsByUserId(UUID usedId) {
        var reservationId = UUID.randomUUID();
        var createdAt = Instant.now();
        var updatedRowCount = jdbcTemplate.update(UPDATE_RESERVATION_IF_EXISTS, createdAt, usedId);
        if (updatedRowCount == 0) {
            jdbcTemplate.update(INSERT_RESERVATION, reservationId, usedId, createdAt);
        }
    }

    @Transactional
    public ReservationInfo getReservationByUserIdForUpdate(UUID usedId) {
        var singleReservationRowList = jdbcTemplate.queryForList(SELECT_RESERVATION_FOR_UPDATE, usedId);
        return new ReservationInfo(
                (UUID) first(singleReservationRowList).get(RESERVATION_ID),
                ((Timestamp) first(singleReservationRowList).get(CREATED_AT)).toInstant()
        );
    }

    private <T> T first(List<T> list) {
        return list.get(0);
    }
}

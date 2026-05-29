package com.zufar.icedlatte.security.session.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zufar.icedlatte.security.session.entity.AuthSessionEntity;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, UUID> {

    Optional<AuthSessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AuthSessionEntity s WHERE s.refreshTokenHash = :refreshTokenHash")
    Optional<AuthSessionEntity> findByRefreshTokenHashForUpdate(@Param("refreshTokenHash") String refreshTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AuthSessionEntity s WHERE s.previousTokenHash = :previousTokenHash")
    Optional<AuthSessionEntity> findByPreviousTokenHashForUpdate(@Param("previousTokenHash") String previousTokenHash);

    @Query("SELECT s FROM AuthSessionEntity s " + "WHERE s.userId = :userId "
            + "AND s.revokedAt IS NULL "
            + "AND s.compromised = false "
            + "AND s.expiresAt > :now")
    List<AuthSessionEntity> findActiveSessions(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE AuthSessionEntity s SET s.revokedAt = :now, s.compromised = true " + "WHERE s.userId = :userId "
            + "AND s.revokedAt IS NULL "
            + "AND s.compromised = false")
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}

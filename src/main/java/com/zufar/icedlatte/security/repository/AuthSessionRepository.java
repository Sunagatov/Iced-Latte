package com.zufar.icedlatte.security.repository;

import com.zufar.icedlatte.security.entity.AuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, UUID> {

    Optional<AuthSessionEntity> findByRefreshTokenHash(String refreshTokenHash);

    @Query("SELECT s FROM AuthSessionEntity s WHERE s.userId = :userId AND s.revokedAt IS NULL AND s.compromised = false AND s.expiresAt > :now")
    List<AuthSessionEntity> findActiveSessions(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("UPDATE AuthSessionEntity s SET s.revokedAt = :now WHERE s.userId = :userId AND s.revokedAt IS NULL AND s.compromised = false")
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}

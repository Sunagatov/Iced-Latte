package com.zufar.onlinestore.security.repository;

import com.zufar.onlinestore.security.entity.LoginAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, UUID> {

    Optional<LoginAttemptEntity> findByUserEmail(String userEmail);

    @Modifying
    @Query("UPDATE LoginAttemptEntity la SET la.isUserLocked = true, la.expirationDatetime = :expiration WHERE la.userEmail = :email")
    void setUserLockedStatusAndExpiration(@Param("email") String userEmail, @Param("expiration") LocalDateTime expirationDatetime);
}

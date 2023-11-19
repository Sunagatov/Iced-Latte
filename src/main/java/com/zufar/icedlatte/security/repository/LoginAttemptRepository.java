package com.zufar.icedlatte.security.repository;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, UUID> {

    Optional<LoginAttemptEntity> findByUserEmail(String userEmail);

    /**
     * Locks the user and sets an expiration datetime for the lock.
     *
     * @param userEmail The email of the user.
     * @param expirationDatetime The expiration time for the lock.
     */
    @Modifying
    @Query("UPDATE LoginAttemptEntity la SET la.isUserLocked = true, la.expirationDatetime = :expiration WHERE la.userEmail = :email")
    void setUserLockedStatusAndExpiration(@Param("email") String userEmail, @Param("expiration") LocalDateTime expirationDatetime);

    /**
     * Resets the locked accounts whose lockout expiration time has passed.
     */
    @Modifying
    @Query("UPDATE LoginAttemptEntity la SET la.attempts = 0, la.isUserLocked = false, la.expirationDatetime = NULL, la.lastModified = CURRENT_TIMESTAMP " +
            "WHERE la.isUserLocked = true AND la.expirationDatetime <= CURRENT_TIMESTAMP")
    void resetLockedAccounts();
}

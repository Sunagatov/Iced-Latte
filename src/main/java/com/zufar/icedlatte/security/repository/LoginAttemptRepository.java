package com.zufar.icedlatte.security.repository;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, UUID> {

    Optional<LoginAttemptEntity> findByUserEmail(String userEmail);

    /**
     * Locks the user and sets an expiration datetime for the lock.
     *
     * @param userEmail          The email of the user.
     * @param expirationDatetime The expiration time for the lock.
     */
    @Modifying(clearAutomatically = true,
            flushAutomatically = true)
    @Query("UPDATE LoginAttemptEntity la " +
            "SET la.isUserLocked = true, la.expirationDatetime = :expiration " +
            "WHERE la.userEmail = :email")
    int setUserLockedStatusAndExpiration(@Param("email") String userEmail,
                                         @Param("expiration") Instant expirationDatetime);

    /**
     * Resets the locked accounts whose lockout expiration time has passed.
     */
    // amazonq-ignore-next-line
    @Modifying(clearAutomatically = true,
            flushAutomatically = true)
    @Query(value = """
            UPDATE login_attempts
            SET attempts = 0,
                is_user_locked = false,
                expiration_datetime = NULL,
                last_modified = CURRENT_TIMESTAMP
            WHERE is_user_locked = true
            AND expiration_datetime <= CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int resetLockedAccounts();
}
package com.zufar.icedlatte.security.signin.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.security.signin.entity.LoginAttemptEntity;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, UUID> {

    Optional<LoginAttemptEntity> findByUserEmail(String userEmail);

    @Query(value = """
                    INSERT INTO login_attempts (id, user_email, attempts, is_user_locked, last_modified)
                    VALUES (gen_random_uuid(), :email, 1, false, :now)
                    ON CONFLICT (user_email) DO UPDATE
                    SET attempts = login_attempts.attempts + 1,
                        last_modified = EXCLUDED.last_modified
                    RETURNING attempts
                    """, nativeQuery = true)
    int recordFailedAttempt(@Param("email") String userEmail, @Param("now") Instant now);

    /**
     * Locks the user and sets an expiration datetime for the lock.
     *
     * @param userEmail The email of the user.
     * @param expirationDatetime The expiration time for the lock.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE LoginAttemptEntity la " + "SET la.isUserLocked = true, la.expirationDatetime = :expiration "
            + "WHERE la.userEmail = :email")
    int setUserLockedStatusAndExpiration(
            @Param("email") String userEmail, @Param("expiration") Instant expirationDatetime);

    @Query("""
            SELECT la.userEmail
            FROM LoginAttemptEntity la
            WHERE la.isUserLocked = true
            AND la.expirationDatetime IS NOT NULL
            AND la.expirationDatetime <= CURRENT_TIMESTAMP
            """)
    List<String> findExpiredLockedUserEmails();

    /** Resets the locked accounts whose lockout expiration time has passed. */
    // amazonq-ignore-next-line
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE LoginAttemptEntity la
            SET la.attempts = 0,
                la.isUserLocked = false,
                la.expirationDatetime = NULL,
                la.lastModified = CURRENT_TIMESTAMP
            WHERE la.isUserLocked = true
            AND la.expirationDatetime <= CURRENT_TIMESTAMP
            """)
    int resetLockedAccounts();
}

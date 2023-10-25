package com.zufar.onlinestore.user.repository;

import com.zufar.onlinestore.user.entity.UserEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    @Modifying
    @Query(value = "UPDATE user_details SET account_non_locked = :accountLocked WHERE email = :email", nativeQuery = true)
    void setAccountLockedStatus(String email, boolean accountLocked);

    @Modifying
    @Query(value = """
            UPDATE user_details
            SET account_non_locked = true
            WHERE account_non_locked = false AND email = ANY (
                UPDATE login_attempts
                SET attempts = 0,
                    is_user_locked = false,
                    expiration_datetime = NULL,
                    last_modified = :last_modified
                WHERE is_user_locked = true AND expiration_datetime > :now
                RETURNING user_email
            )
            """, nativeQuery = true)
    void unlockLockoutExpiredAccounts(
            @Param("last_modified") LocalDateTime lastModified,
            @Param("now") LocalDateTime now
    );
}

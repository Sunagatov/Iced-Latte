package com.zufar.onlinestore.user.repository;

import com.zufar.onlinestore.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Finds a user entity by its email.
     *
     * @param email The email of the user.
     * @return An optional containing the user entity if found.
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Finds a user entity by its confirmation token.
     *
     * @param token The confirmation token of the user.
     * @return An optional containing the user entity if found.
     */
    Optional<UserEntity> findByConfirmationToken(String token);

    /**
     * Updates the locked status of a user based on the given email.
     *
     * @param email The email of the user.
     * @param accountLocked The new locked status to set.
     */
    @Modifying
    @Query("UPDATE UserEntity u SET u.accountNonLocked = :accountLocked WHERE u.email = :email")
    void setAccountLockedStatus(@Param("email") String email, @Param("accountLocked") boolean accountLocked);

    /**
     * Unlocks all users that have corresponding entries in the login_attempts table
     * with is_user_locked set to false.
     */
    @Modifying
    @Query(value = "UPDATE UserEntity u SET u.accountNonLocked = true WHERE u.email IN (SELECT la.userEmail FROM LoginAttemptEntity la WHERE la.isUserLocked = false)")
    void unlockUsers();
}

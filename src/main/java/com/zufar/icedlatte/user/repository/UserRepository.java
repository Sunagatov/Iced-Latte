package com.zufar.icedlatte.user.repository;

import com.zufar.icedlatte.user.entity.UserEntity;
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
     * Updates the locked status of a user based on the given email.
     *
     * @param newPassword The new password of the user.
     * @param userId      The id of the user.
     */
    @Modifying
    @Query(value = "UPDATE UserEntity u SET u.password = :newPassword WHERE u.id = :userId")
    void changeUserPassword(@Param("newPassword") String newPassword, @Param("userId") UUID userId);

    /**
     * Updates the locked status of a user based on the given email.
     *
     * @param email         The email of the user.
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

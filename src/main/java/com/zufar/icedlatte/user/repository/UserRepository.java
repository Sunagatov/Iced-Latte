package com.zufar.icedlatte.user.repository;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zufar.icedlatte.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Finds a user entity by its email.
     *
     * @param email The email of the user.
     * @return An optional containing the user entity if found.
     */
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "authorities")
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email")
    Optional<UserEntity> findByEmailWithAuthorities(@Param("email") String email);

    @EntityGraph(attributePaths = "authorities")
    @Query("SELECT u FROM UserEntity u WHERE u.id = :userId")
    Optional<UserEntity> findByIdWithAuthorities(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.id = :userId")
    Optional<UserEntity> findByIdForUpdate(@Param("userId") UUID userId);

    /**
     * Updates the password of a user by id.
     *
     * @param newPassword The new password of the user.
     * @param userId The id of the user.
     */
    @Modifying
    @Query(value = "UPDATE UserEntity u " + "SET u.password = :newPassword " + "WHERE u.id = :userId")
    int changeUserPassword(@Param("newPassword") String newPassword, @Param("userId") UUID userId);

    /**
     * Updates the accountNonLocked status of a user based on the given email.
     *
     * @param email The email of the user.
     * @param accountNonLocked The new accountNonLocked value (true = unlocked, false = locked).
     */
    @Modifying
    @Query("UPDATE UserEntity u " + "SET u.accountNonLocked = :accountNonLocked " + "WHERE u.email = :email")
    int setAccountNonLockedStatus(@Param("email") String email, @Param("accountNonLocked") boolean accountNonLocked);
}

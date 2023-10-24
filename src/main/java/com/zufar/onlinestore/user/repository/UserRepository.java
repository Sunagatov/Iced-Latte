package com.zufar.onlinestore.user.repository;

import com.zufar.onlinestore.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    @Modifying
    @Query(value = "UPDATE user_details SET account_non_locked = :accountLocked WHERE email = :email", nativeQuery = true)
    void setAccountLockedStatus(String email, boolean accountLocked);
}

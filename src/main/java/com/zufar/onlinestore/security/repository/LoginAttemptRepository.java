package com.zufar.onlinestore.security.repository;

import com.zufar.onlinestore.security.entity.LoginAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, String> {

    Optional<LoginAttemptEntity> findByUserEmail(String userEmail);

    void deleteByUserEmail(String userEmail);
}

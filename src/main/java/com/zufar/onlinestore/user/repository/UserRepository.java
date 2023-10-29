package com.zufar.onlinestore.user.repository;

import com.zufar.onlinestore.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    @Query(nativeQuery = true, value = """
            SELECT u.password FROM user_details u
            WHERE u.email = ?1
            """)
    Optional<String> findPasswordByEmail(@Param("email") String email);

}

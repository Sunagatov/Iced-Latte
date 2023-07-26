package com.zufar.onlinestore.user.repository;

import com.zufar.onlinestore.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    UserEntity findUserByUserName(String userName);
}

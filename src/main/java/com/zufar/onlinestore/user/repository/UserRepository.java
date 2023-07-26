package com.zufar.onlinestore.user.repository;

import com.zufar.onlinestore.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}

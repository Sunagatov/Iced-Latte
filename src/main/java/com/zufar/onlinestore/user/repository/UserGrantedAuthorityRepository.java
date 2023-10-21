package com.zufar.onlinestore.user.repository;

import com.zufar.onlinestore.user.entity.UserGrantedAuthority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserGrantedAuthorityRepository extends JpaRepository<UserGrantedAuthority, UUID> {

}

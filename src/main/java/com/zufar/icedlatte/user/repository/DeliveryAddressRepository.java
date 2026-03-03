package com.zufar.icedlatte.user.repository;

import com.zufar.icedlatte.user.entity.DeliveryAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddressEntity, UUID> {

    List<DeliveryAddressEntity> findAllByUserId(UUID userId);

    Optional<DeliveryAddressEntity> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE DeliveryAddressEntity a SET a.isDefault = false WHERE a.user.id = :userId")
    void clearDefaultForUser(@Param("userId") UUID userId);
}

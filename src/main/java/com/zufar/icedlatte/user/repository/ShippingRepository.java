package com.zufar.icedlatte.user.repository;

import com.zufar.icedlatte.payment.entity.Shipping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShippingRepository extends JpaRepository<Shipping, Long> {

    @Query(value = "select u from UserEntity u where u.id = :userId")
    List<Shipping> findDeliveriesByUserId(UUID userId);

    Shipping findByShippingId(Long shippingId);

}

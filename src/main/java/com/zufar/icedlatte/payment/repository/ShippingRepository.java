package com.zufar.icedlatte.payment.repository;

import com.zufar.icedlatte.payment.entity.Shipping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShippingRepository extends JpaRepository<Shipping, Long> {

    @Query(value = "select s from Shipping s where s.user.id = :userId")
    List<Shipping> findDeliveriesByUserId(UUID userId);

    Shipping findByShippingId(Long shippingId);

}

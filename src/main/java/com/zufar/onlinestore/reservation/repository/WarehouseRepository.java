package com.zufar.onlinestore.reservation.repository;

import com.zufar.onlinestore.reservation.entity.Warehouse;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
}
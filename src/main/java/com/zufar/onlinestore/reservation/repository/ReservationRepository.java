package com.zufar.onlinestore.reservation.repository;

import com.zufar.onlinestore.reservation.entity.Reservation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    @Query(value = "SELECT * FROM reservation WHERE reservation_id = :id", nativeQuery = true)
    List<Reservation> findAllByReservationId(@Param("id") UUID reservationId);
}
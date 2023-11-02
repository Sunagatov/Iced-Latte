package com.zufar.onlinestore.payment.repository;

import com.zufar.onlinestore.payment.entity.Payment;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, Long> {

    @Modifying
    @Query(value = "UPDATE payment SET status = :payment_status, description = :payment_description WHERE payment_intent_id = :payment_intent_id",
            nativeQuery = true)
    void updateStatusAndDescriptionInPayment(@Param("payment_intent_id") String paymentIntentId,
                                             @Param("payment_status") String paymentStatus,
                                             @Param("payment_description") String paymentDescription);

}

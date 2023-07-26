package com.zufar.onlinestore.payment.repository;

import com.zufar.onlinestore.payment.entity.Payment;
import com.zufar.onlinestore.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends CrudRepository<Payment, Long> {

    @Modifying
    @Query(value = "UPDATE payment SET status = :payment_status, description = :payment_description" +
            " WHERE payment_intent_id = :payment_intent_id RETURNING *",
            nativeQuery = true)
    Payment updateStatusAndDescriptionInPayment(@Param("payment_intent_id") String paymentIntentId,
                                                @Param("payment_status") PaymentStatus paymentStatus,
                                                @Param("payment_description") String paymentDescription);

}

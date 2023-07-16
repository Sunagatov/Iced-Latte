package com.zufar.onlinestore.payment.repository;

import com.zufar.onlinestore.payment.model.Payment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, String> {
}

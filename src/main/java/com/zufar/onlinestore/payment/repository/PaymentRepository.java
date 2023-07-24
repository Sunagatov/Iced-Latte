package com.zufar.onlinestore.payment.repository;

import com.zufar.onlinestore.payment.entity.Payment;
import org.springframework.data.repository.CrudRepository;

public interface PaymentRepository extends CrudRepository<Payment, Long> {
}

package com.zufar.icedlatte.payment.repository;

import com.zufar.icedlatte.payment.entity.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, String> {
}

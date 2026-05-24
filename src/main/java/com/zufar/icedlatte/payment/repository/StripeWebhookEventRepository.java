package com.zufar.icedlatte.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zufar.icedlatte.payment.entity.StripeWebhookEvent;

@SuppressWarnings("unused") // Spring Data generates implementations for repository methods.
public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, String> {}

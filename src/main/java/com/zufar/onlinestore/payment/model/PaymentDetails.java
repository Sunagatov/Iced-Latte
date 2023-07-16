package com.zufar.onlinestore.payment.model;

import lombok.Builder;

@Builder
public record PaymentDetails(String paymentToken, Payment payment) {}

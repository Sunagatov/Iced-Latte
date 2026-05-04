package com.zufar.icedlatte.payment.api;

public record StripeSessionResult(String sessionId, String checkoutUrl) {
}

package com.zufar.icedlatte.payment.api.checkout;

import java.util.Objects;

public record StripeSessionResult(String sessionId, String checkoutUrl) {
    public StripeSessionResult {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(checkoutUrl, "checkoutUrl");
    }
}

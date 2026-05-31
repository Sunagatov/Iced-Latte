package com.zufar.icedlatte.payment.service;

import org.jspecify.annotations.Nullable;

public record PaymentConfirmationSource(
        @Nullable String eventId, String eventType, String confirmationReason) {}

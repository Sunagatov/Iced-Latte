package com.zufar.icedlatte.supportchat.service;

import org.jspecify.annotations.Nullable;

public record SupportChatEligibility(
        boolean eligible, @Nullable String reason) {

    public static SupportChatEligibility createEligible() {
        return new SupportChatEligibility(true, null);
    }

    public static SupportChatEligibility emailVerificationRequired() {
        return new SupportChatEligibility(false, "EMAIL_VERIFICATION_REQUIRED");
    }
}

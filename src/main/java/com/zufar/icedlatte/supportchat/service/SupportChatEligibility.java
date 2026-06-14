package com.zufar.icedlatte.supportchat.service;

import org.jspecify.annotations.Nullable;

public record SupportChatEligibility(boolean eligible, @Nullable String reason) {

    public static final String REASON_ACCESS_RESTRICTED = "ACCESS_RESTRICTED";
    public static final String REASON_EMAIL_VERIFICATION_REQUIRED = "EMAIL_VERIFICATION_REQUIRED";

    public static SupportChatEligibility createEligible() {
        return new SupportChatEligibility(true, null);
    }

    public static SupportChatEligibility emailVerificationRequired() {
        return new SupportChatEligibility(false, REASON_EMAIL_VERIFICATION_REQUIRED);
    }

    public static SupportChatEligibility accessRestricted() {
        return new SupportChatEligibility(false, REASON_ACCESS_RESTRICTED);
    }

    public boolean isAccessRestricted() {
        return REASON_ACCESS_RESTRICTED.equals(reason);
    }
}

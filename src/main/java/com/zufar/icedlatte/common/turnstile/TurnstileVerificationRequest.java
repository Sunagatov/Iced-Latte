package com.zufar.icedlatte.common.turnstile;

import jakarta.annotation.Nullable;

public record TurnstileVerificationRequest(
        @Nullable String token,
        @Nullable String remoteIp,
        String source,
        @Nullable String expectedAction) {

    public TurnstileVerificationRequest {
        source = source == null || source.isBlank() ? "unspecified" : source.trim();
        remoteIp = normalize(remoteIp);
        expectedAction = normalize(expectedAction);
    }

    public static TurnstileVerificationRequest basic(@Nullable String token, @Nullable String remoteIp) {
        return new TurnstileVerificationRequest(token, remoteIp, "unspecified", null);
    }

    public static TurnstileVerificationRequest forAction(
            @Nullable String token, @Nullable String remoteIp, String action) {
        return new TurnstileVerificationRequest(token, remoteIp, action, action);
    }

    private static @Nullable String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

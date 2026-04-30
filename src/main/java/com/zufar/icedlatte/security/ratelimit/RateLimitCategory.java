package com.zufar.icedlatte.security.ratelimit;

public enum RateLimitCategory {
    GLOBAL("global"),
    AUTH("auth"),
    SEARCH("search"),
    TELEMETRY("telemetry"),
    PAYMENT("payment"),
    PRE_AUTH("pre-auth"),
    AUTH_PRE("auth-pre");

    private final String value;

    RateLimitCategory(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}

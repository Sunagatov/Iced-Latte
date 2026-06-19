package com.zufar.icedlatte.ratelimit.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RateLimitCategory {
    GLOBAL("global"),
    AUTH("auth"),
    LOGIN("login"),
    SIGNUP("signup"),
    PASSWORD_RESET("password-reset"),
    SEARCH("search"),
    TELEMETRY("telemetry"),
    PAYMENT("payment"),
    CHECKOUT("checkout"),
    REVIEW_WRITE("review-write"),
    WRITE("write"),
    FILE_UPLOAD("file-upload"),
    PRE_AUTH("pre-auth"),
    AUTH_PRE("auth-pre");

    private final String value;
}

package com.zufar.icedlatte.ratelimit.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RateLimitCategory {
    GLOBAL("global"),
    AUTH("auth"),
    SEARCH("search"),
    TELEMETRY("telemetry"),
    PAYMENT("payment"),
    WRITE("write"),
    FILE_UPLOAD("file-upload"),
    PRE_AUTH("pre-auth"),
    AUTH_PRE("auth-pre");

    private final String value;
}

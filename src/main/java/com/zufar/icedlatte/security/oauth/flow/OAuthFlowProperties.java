package com.zufar.icedlatte.security.oauth.flow;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "oauth")
public record OAuthFlowProperties(
        Integer stateTtlMinutes,

        @NotNull(message = "OAuth handoff TTL must not be null")
        Duration handoffTtl,

        String handoffEncryptionKey) {

    public OAuthFlowProperties {
        if (stateTtlMinutes == null) {
            throw new IllegalArgumentException("oauth.state-ttl-minutes must not be null");
        }
        if (stateTtlMinutes < 1) {
            throw new IllegalArgumentException("oauth.state-ttl-minutes must be at least 1 minute");
        }
        if (handoffTtl == null) {
            throw new IllegalArgumentException("oauth.handoff-ttl must not be null");
        }
        if (handoffTtl.isZero() || handoffTtl.isNegative()) {
            throw new IllegalArgumentException("oauth.handoff-ttl must be positive");
        }
    }
}

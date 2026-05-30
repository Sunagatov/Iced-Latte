package com.zufar.icedlatte.security.service.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "security.temporary-store")
public record TemporaryStoreProperties(@DefaultValue("redis") Mode mode) {

    public TemporaryStoreProperties {
        if (mode == null) {
            throw new IllegalArgumentException("security.temporary-store.mode must be configured");
        }
    }

    public TemporaryStoreProperties() {
        this(Mode.REDIS);
    }

    public enum Mode {
        REDIS,
        MEMORY
    }
}

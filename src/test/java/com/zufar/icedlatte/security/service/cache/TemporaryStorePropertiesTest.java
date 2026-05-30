package com.zufar.icedlatte.security.service.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TemporaryStoreProperties")
class TemporaryStorePropertiesTest {

    @Test
    @DisplayName("defaults to redis")
    void defaultsToRedis() {
        assertThat(new TemporaryStoreProperties().mode()).isEqualTo(TemporaryStoreProperties.Mode.REDIS);
    }

    @Test
    @DisplayName("rejects null mode")
    void rejectsNullMode() {
        assertThatThrownBy(() -> new TemporaryStoreProperties(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("security.temporary-store.mode must be configured");
    }
}

package com.zufar.icedlatte.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtBlacklistService")
class JwtBlacklistServiceTest {

    private final JwtBlacklistService service = new JwtBlacklistService() {
        @Override
        public void blacklistToken(String token) {
        }

        @Override
        public boolean isBlacklisted(String token) {
            return false;
        }
    };

    @Test
    @DisplayName("sha256 hashes tokens deterministically")
    void sha256HashesTokensDeterministically() {
        assertThat(service.sha256("test.jwt.token"))
                .isEqualTo("65174034c8b6eecca89abd6f0400b14cddb6a78b2128c7933b89eeafb3c73281");
        assertThat(service.sha256("test.jwt.token"))
                .isEqualTo(service.sha256("test.jwt.token"));
        assertThat(service.sha256("another.token"))
                .isNotEqualTo(service.sha256("test.jwt.token"));
    }
}

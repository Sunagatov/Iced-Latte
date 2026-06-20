package com.zufar.icedlatte.security.jwt.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@DisplayName("JwtSigningKeys unit tests")
class JwtSigningKeysTest {

    @Test
    @DisplayName("builds separate signing keys from the configured access and refresh secrets")
    void buildsSeparateSigningKeysFromConfiguredSecrets() {
        String secret = Base64.getEncoder().encodeToString(new byte[64]);
        byte[] refreshBytes = new byte[64];
        refreshBytes[0] = 1;
        String refreshSecret = Base64.getEncoder().encodeToString(refreshBytes);
        JwtProperties properties = new JwtProperties(
                "Authorization",
                secret,
                refreshSecret,
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                "iced-latte",
                "iced-latte-client",
                "access-active",
                "refresh-active",
                null,
                null,
                null,
                null);

        JwtSigningKeys provider = new JwtSigningKeys(properties);

        SecretKey expectedAccess = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        SecretKey expectedRefresh = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));

        assertThat(provider.get().getEncoded()).isEqualTo(expectedAccess.getEncoded());
        assertThat(provider.getRefresh().getEncoded()).isEqualTo(expectedRefresh.getEncoded());
        assertThat(provider.get().getEncoded())
                .isNotEqualTo(provider.getRefresh().getEncoded());
        assertThat(provider.getKeyId()).isEqualTo("access-active");
        assertThat(provider.getRefreshKeyId()).isEqualTo("refresh-active");
    }

    @Test
    @DisplayName("resolves previous signing keys by kid during rotation")
    void resolvesPreviousSigningKeysByKid() {
        String secret = Base64.getEncoder().encodeToString(new byte[64]);
        byte[] refreshBytes = new byte[64];
        refreshBytes[0] = 1;
        String refreshSecret = Base64.getEncoder().encodeToString(refreshBytes);
        byte[] previousAccessBytes = new byte[64];
        previousAccessBytes[1] = 2;
        byte[] previousRefreshBytes = new byte[64];
        previousRefreshBytes[2] = 3;
        JwtProperties properties = new JwtProperties(
                "Authorization",
                secret,
                refreshSecret,
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                "iced-latte",
                "iced-latte-client",
                "access-active",
                "refresh-active",
                Base64.getEncoder().encodeToString(previousAccessBytes),
                Base64.getEncoder().encodeToString(previousRefreshBytes),
                "access-previous",
                "refresh-previous");

        JwtSigningKeys provider = new JwtSigningKeys(properties);

        assertThat(provider.resolveAccessVerificationKey("access-previous").getEncoded())
                .isEqualTo(Keys.hmacShaKeyFor(previousAccessBytes).getEncoded());
        assertThat(provider.resolveRefreshVerificationKey("refresh-previous").getEncoded())
                .isEqualTo(Keys.hmacShaKeyFor(previousRefreshBytes).getEncoded());
    }

    @Test
    @DisplayName("rejects identical access and refresh signing keys")
    void rejectsIdenticalSigningKeys() {
        String secret = Base64.getEncoder().encodeToString(new byte[64]);
        JwtProperties properties = new JwtProperties(
                "Authorization",
                secret,
                secret,
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                "iced-latte",
                "iced-latte-client",
                null,
                null,
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> new JwtSigningKeys(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be different");
    }
}

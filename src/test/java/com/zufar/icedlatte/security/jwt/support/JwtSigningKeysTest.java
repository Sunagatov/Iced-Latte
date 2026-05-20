package com.zufar.icedlatte.security.jwt.support;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

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
                "iced-latte-client");

        JwtSigningKeys provider = new JwtSigningKeys(properties);

        SecretKey expectedAccess = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        SecretKey expectedRefresh = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));

        assertThat(provider.get().getEncoded()).isEqualTo(expectedAccess.getEncoded());
        assertThat(provider.getRefresh().getEncoded()).isEqualTo(expectedRefresh.getEncoded());
        assertThat(provider.get().getEncoded()).isNotEqualTo(provider.getRefresh().getEncoded());
    }
}

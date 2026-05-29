package com.zufar.icedlatte.security.jwt.config;

import java.util.Arrays;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtSigningKeys {

    private final SecretKey signingKey;
    private final SecretKey refreshKey;

    public JwtSigningKeys(JwtProperties jwtProperties) {
        byte[] accessKeyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        byte[] refreshKeyBytes = Decoders.BASE64.decode(jwtProperties.refreshSecret());
        if (Arrays.equals(accessKeyBytes, refreshKeyBytes)) {
            throw new IllegalStateException("JWT access and refresh signing keys must be different");
        }
        this.signingKey = Keys.hmacShaKeyFor(accessKeyBytes);
        this.refreshKey = Keys.hmacShaKeyFor(refreshKeyBytes);
    }

    public SecretKey get() {
        return signingKey;
    }

    public SecretKey getRefresh() {
        return refreshKey;
    }
}

package com.zufar.icedlatte.security.jwt.config;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtSigningKeys {

    private final SecretKey signingKey;
    private final SecretKey refreshKey;

    public JwtSigningKeys(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.refreshSecret()));
    }

    public SecretKey get() {
        return signingKey;
    }

    public SecretKey getRefresh() {
        return refreshKey;
    }
}

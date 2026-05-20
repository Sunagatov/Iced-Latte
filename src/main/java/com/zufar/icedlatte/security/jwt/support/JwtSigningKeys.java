package com.zufar.icedlatte.security.jwt.support;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

@Service
public class JwtSigningKeys {

    private final SecretKey signingKey;
    private final SecretKey refreshKey;

    public JwtSigningKeys(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.refreshSecret()));
    }

    public SecretKey get() { return signingKey; }

    public SecretKey getRefresh() { return refreshKey; }
}

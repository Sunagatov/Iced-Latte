package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

@Service
@RequiredArgsConstructor
public class JwtSignKeyProvider {

    private final JwtProperties jwtProperties;

    public SecretKey get() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public SecretKey getRefresh() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.refreshSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

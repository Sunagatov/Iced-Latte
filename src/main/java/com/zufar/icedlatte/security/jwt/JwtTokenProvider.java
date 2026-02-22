package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import com.zufar.icedlatte.security.exception.JwtTokenException;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtSignKeyProvider jwtSignKeyProvider;
    private final JwtProperties jwtProperties;

    public String generateToken(final UserDetails userDetails) {
        return generateToken(Map.of(), userDetails);
    }

    public String generateToken(final Map<String, Object> extraClaims,
                               final UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtProperties.expiration(), jwtSignKeyProvider.get());
    }

    public String generateRefreshToken(final UserDetails userDetails) {
        return buildToken(Map.of(), userDetails, jwtProperties.refreshExpiration(), jwtSignKeyProvider.getRefresh());
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails,
                              Duration expiration, SecretKey key) {
        try {
            Instant now = Instant.now();
            return Jwts.builder()
                    .claims(extraClaims)
                    .subject(userDetails.getUsername())
                    .issuer(jwtProperties.issuer())
                    .audience().add(jwtProperties.audience()).and()
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plus(expiration)))
                    .signWith(key)
                    .compact();
        } catch (JwtException exception) {
            log.error("jwt.create.error", exception);
            throw new JwtTokenException(exception);
        }
    }
}

package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import com.zufar.icedlatte.security.exception.JwtTokenException;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

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
        try {
            Instant now = Instant.now();
            return Jwts.builder()
                    .claims(extraClaims)
                    .subject(userDetails.getUsername())
                    .issuer(jwtProperties.issuer())
                    .audience().add(jwtProperties.audience()).and()
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plus(jwtProperties.expiration())))
                    .signWith(jwtSignKeyProvider.get())
                    .compact();
        } catch (JwtException exception) {
            log.error("JWT token creation failed for user: {}", userDetails.getUsername(), exception);
            throw new JwtTokenException(exception);
        }
    }

    public String generateRefreshToken(final UserDetails userDetails) {
        try {
            Instant now = Instant.now();
            return Jwts.builder()
                    .subject(userDetails.getUsername())
                    .issuer(jwtProperties.issuer())
                    .audience().add(jwtProperties.audience()).and()
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plus(jwtProperties.refreshExpiration())))
                    .signWith(jwtSignKeyProvider.getRefresh())
                    .compact();
        } catch (JwtException exception) {
            log.error("JWT refresh token creation failed for user: {}", userDetails.getUsername(), exception);
            throw new JwtTokenException(exception);
        }
    }
}

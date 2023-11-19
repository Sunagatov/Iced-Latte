package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtClaimExtractor {
    private final JwtSignKeyProvider jwtSignKeyProvider;

    public String extractEmail(final String jwtToken) {
        String userEmail = extractAllClaims(jwtToken).getSubject();

        if (StringUtils.isEmpty(userEmail))
            throw new JwtTokenHasNoUserEmailException();
        else
            return userEmail;
    }

    public LocalDateTime extractExpiration(final String jwtToken) {
        Date expiration = extractAllClaims(jwtToken)
                .getExpiration();

        return Instant
                .ofEpochMilli(expiration.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private Claims extractAllClaims(final String jwtToken) {
        return getJwtParser()
                .parseClaimsJws(jwtToken)
                .getBody();
    }

    private JwtParser getJwtParser() {
        return Jwts
                .parserBuilder()
                .setSigningKey(jwtSignKeyProvider.get())
                .build();
    }
}

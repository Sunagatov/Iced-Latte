package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenHasNoUserEmailException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtClaimExtractor {
    private final JwtSignKeyProvider jwtSignKeyProvider;

    public String extractEmail(final String jwtToken) {
        return Optional.ofNullable(extractAllClaims(jwtToken).getSubject())
                .filter(StringUtils::hasText)
                .orElseThrow(JwtTokenHasNoUserEmailException::new);
    }

    public LocalDateTime extractExpiration(final String jwtToken) {
        Date expiration = extractAllClaims(jwtToken).getExpiration();
        return expiration.toInstant()
                .atOffset(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    public boolean isTokenExpired(final String jwtToken) {
        return extractExpiration(jwtToken).isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private Claims extractAllClaims(final String jwtToken) {
        return getJwtParser()
                .parseSignedClaims(jwtToken)
                .getPayload();
    }

    private JwtParser getJwtParser() {
        return Jwts
                .parser()
                .verifyWith(jwtSignKeyProvider.get())
                .build();
    }
}

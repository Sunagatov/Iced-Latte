package com.zufar.icedlatte.security.jwt.provider;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.zufar.icedlatte.security.jwt.config.JwtClaimNames;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.jwt.config.JwtSigningKeys;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenException;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtSigningKeys jwtSigningKeys;
    private final JwtProperties jwtProperties;

    public String generateToken(final UserDetails userDetails, UUID sessionId) {
        return generateToken(Map.of(), userDetails, sessionId);
    }

    public String generateToken(final Map<String, Object> extraClaims, final UserDetails userDetails, UUID sessionId) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(JwtClaimNames.JWT_ID, UUID.randomUUID().toString());
        claims.put(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.ACCESS_TOKEN_PURPOSE);
        if (sessionId != null) {
            claims.put(JwtClaimNames.SESSION_ID, sessionId.toString());
        }
        return buildToken(claims, userDetails, jwtProperties.expiration(), jwtSigningKeys.get());
    }

    public String generateRefreshToken(final UserDetails userDetails, UUID sessionId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimNames.JWT_ID, UUID.randomUUID().toString());
        claims.put(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.REFRESH_TOKEN_PURPOSE);
        claims.put(JwtClaimNames.VERSION, 2);
        if (sessionId != null) {
            claims.put(JwtClaimNames.SESSION_ID, sessionId.toString());
        }
        return buildToken(claims, userDetails, jwtProperties.refreshExpiration(), jwtSigningKeys.getRefresh());
    }

    public String generateSupportChatWebSocketTicket(final String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimNames.JWT_ID, UUID.randomUUID().toString());
        claims.put(JwtClaimNames.TOKEN_PURPOSE, JwtClaimNames.SUPPORT_CHAT_WEBSOCKET_TICKET_PURPOSE);
        return buildToken(claims, email, Duration.ofMinutes(2), jwtSigningKeys.get());
    }

    private String buildToken(
            Map<String, Object> extraClaims, UserDetails userDetails, Duration expiration, SecretKey key) {
        return buildToken(extraClaims, userDetails.getUsername(), expiration, key);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, Duration expiration, SecretKey key) {
        try {
            Instant now = Instant.now();

            return Jwts.builder()
                    .claims(extraClaims)
                    .subject(subject)
                    .issuer(jwtProperties.issuer())
                    .audience()
                    .add(jwtProperties.audience())
                    .and()
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plus(expiration)))
                    .signWith(key)
                    .compact();

        } catch (JwtException exception) {
            log.error("jwt.create.error: message={}", exception.getMessage(), exception);
            throw new JwtTokenException(exception);
        }
    }
}

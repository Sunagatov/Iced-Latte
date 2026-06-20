package com.zufar.icedlatte.security.jwt.resolver;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.security.jwt.config.JwtClaimNames;
import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.jwt.config.JwtSigningKeys;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ProtectedHeader;
import io.jsonwebtoken.security.SecurityException;

@Service
public class JwtTokenClaims {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtParser accessTokenParser;
    private final JwtParser previousAccessTokenParser;
    private final JwtParser refreshTokenParser;
    private final JwtParser previousRefreshTokenParser;
    private final JwtParser supportChatWebSocketTicketParser;
    private final JwtParser previousSupportChatWebSocketTicketParser;

    public JwtTokenClaims(JwtSigningKeys jwtSigningKeys, JwtProperties jwtProperties) {
        this.accessTokenParser = buildParser(
                jwtProperties, JwtClaimNames.ACCESS_TOKEN_PURPOSE, jwtSigningKeys::resolveAccessVerificationKey);
        this.previousAccessTokenParser =
                buildLegacyParser(jwtSigningKeys.getPrevious(), jwtProperties, JwtClaimNames.ACCESS_TOKEN_PURPOSE);
        this.refreshTokenParser = buildParser(
                jwtProperties, JwtClaimNames.REFRESH_TOKEN_PURPOSE, jwtSigningKeys::resolveRefreshVerificationKey);
        this.previousRefreshTokenParser = buildLegacyParser(
                jwtSigningKeys.getPreviousRefresh(), jwtProperties, JwtClaimNames.REFRESH_TOKEN_PURPOSE);
        this.supportChatWebSocketTicketParser = buildParser(
                jwtProperties,
                JwtClaimNames.SUPPORT_CHAT_WEBSOCKET_TICKET_PURPOSE,
                jwtSigningKeys::resolveAccessVerificationKey);
        this.previousSupportChatWebSocketTicketParser = buildLegacyParser(
                jwtSigningKeys.getPrevious(), jwtProperties, JwtClaimNames.SUPPORT_CHAT_WEBSOCKET_TICKET_PURPOSE);
    }

    public String extractAccessTokenEmail(final String token) {
        try {
            return extractEmail(accessTokenClaims(token), "Missing email in JWT token");
        } catch (ExpiredJwtException | JwtTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtTokenException("Failed to extract email from JWT token", ex);
        }
    }

    public Optional<UUID> extractAccessTokenSessionId(final String token) {
        try {
            return extractSessionId(accessTokenClaims(token));
        } catch (Exception _) {
            return Optional.empty();
        }
    }

    public String extractRefreshTokenEmail(final String token) {
        try {
            return extractEmail(refreshTokenClaims(token), "Refresh token has no subject");
        } catch (ExpiredJwtException | JwtTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtTokenException("Invalid refresh token", ex);
        }
    }

    public String extractSupportChatWebSocketTicketEmail(final String token) {
        try {
            return extractEmail(
                    supportChatWebSocketTicketClaims(token), "Support chat WebSocket ticket has no subject");
        } catch (ExpiredJwtException | JwtTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JwtTokenException("Invalid support chat WebSocket ticket", ex);
        }
    }

    public boolean isSessionManagedRefreshToken(final String token) {
        try {
            return refreshTokenClaims(token).get(JwtClaimNames.VERSION) != null;
        } catch (Exception _) {
            return false;
        }
    }

    public Optional<UUID> extractRefreshTokenSessionId(final String token) {
        try {
            return extractSessionId(refreshTokenClaims(token));
        } catch (Exception _) {
            return Optional.empty();
        }
    }

    private String extractEmail(Claims claims, String missingEmailMessage) {
        return Optional.ofNullable(claims.getSubject())
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new JwtTokenException(missingEmailMessage));
    }

    private Optional<UUID> extractSessionId(Claims claims) {
        String sessionId = (String) claims.get(JwtClaimNames.SESSION_ID);
        return StringUtils.hasText(sessionId) ? Optional.of(UUID.fromString(sessionId)) : Optional.empty();
    }

    private Claims accessTokenClaims(final String token) {
        return parseClaims(token, accessTokenParser, previousAccessTokenParser);
    }

    private Claims refreshTokenClaims(final String token) {
        return parseClaims(token, refreshTokenParser, previousRefreshTokenParser);
    }

    private Claims supportChatWebSocketTicketClaims(final String token) {
        return parseClaims(token, supportChatWebSocketTicketParser, previousSupportChatWebSocketTicketParser);
    }

    private JwtParser buildParser(JwtProperties jwtProperties, String tokenPurpose, Function<String, Key> keyResolver) {
        return Jwts.parser()
                .keyLocator(header -> keyResolver.apply(extractKeyId(header)))
                .requireIssuer(jwtProperties.issuer())
                .requireAudience(jwtProperties.audience())
                .require(JwtClaimNames.TOKEN_PURPOSE, tokenPurpose)
                .build();
    }

    private String extractKeyId(Header header) {
        if (header instanceof ProtectedHeader protectedHeader) {
            return protectedHeader.getKeyId();
        }
        return null;
    }

    private JwtParser buildLegacyParser(SecretKey key, JwtProperties jwtProperties, String tokenPurpose) {
        if (key == null) {
            return null;
        }
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(jwtProperties.issuer())
                .requireAudience(jwtProperties.audience())
                .require(JwtClaimNames.TOKEN_PURPOSE, tokenPurpose)
                .build();
    }

    private Claims parseClaims(String token, JwtParser primaryParser, JwtParser legacyParser) {
        try {
            return primaryParser.parseSignedClaims(token).getPayload();
        } catch (RuntimeException primaryFailure) {
            if (legacyParser == null || tokenHasKeyId(token) || !(primaryFailure instanceof SecurityException)) {
                throw primaryFailure;
            }
            return legacyParser.parseSignedClaims(token).getPayload();
        }
    }

    private boolean tokenHasKeyId(String token) {
        String[] parts = token.split("\\.", 3);
        if (parts.length < 2) {
            return false;
        }
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readTree(headerJson).hasNonNull("kid");
        } catch (Exception ex) {
            return false;
        }
    }
}

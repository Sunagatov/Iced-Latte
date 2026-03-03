package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenFromAuthHeaderExtractor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MAX_TOKEN_LENGTH = 2048;
    private static final int MIN_TOKEN_LENGTH = 10;

    private final JwtProperties jwtProperties;

    public String extract(final HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.header());
        return extract(header);
    }

    public String extract(final String header) {
        return Optional.ofNullable(header)
                .filter(StringUtils::hasText)
                .filter(this::isValidBearerHeader)
                .map(this::extractTokenFromHeader)
                .filter(this::isValidTokenFormat)
                .orElseThrow(() -> {
                    log.debug("jwt.header.invalid");
                    return new AbsentBearerHeaderException(
                        "Missing or invalid Authorization header. Expected format: " + BEARER_PREFIX + "<token>"
                    );
                });
    }

    private boolean isValidBearerHeader(String authHeader) {
        return authHeader.startsWith(BEARER_PREFIX) && authHeader.length() > BEARER_PREFIX.length();
    }

    private String extractTokenFromHeader(String authHeader) {
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    private boolean isValidTokenFormat(String token) {
        if (!StringUtils.hasText(token)) {
            log.debug("jwt.header.empty_token");
            return false;
        }
        
        if (token.length() < MIN_TOKEN_LENGTH || token.length() > MAX_TOKEN_LENGTH) {
            log.debug("jwt.header.invalid_length");
            return false;
        }
        
        // Basic JWT format validation (should have 2 dots for 3 parts)
        long dotCount = token.chars().filter(ch -> ch == '.').count();
        if (dotCount != 2) {
            log.debug("jwt.header.invalid_format");
            return false;
        }
        
        return true;
    }
}

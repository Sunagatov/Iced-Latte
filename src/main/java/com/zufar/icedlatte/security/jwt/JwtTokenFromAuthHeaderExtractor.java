package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

@Service
public class JwtTokenFromAuthHeaderExtractor {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.header:Authorization}")
    private String jwtHttpRequestHeader;

    public String extract(final HttpServletRequest request) {
        String header = request.getHeader(jwtHttpRequestHeader);
        return extract(header);
    }

    public String extract(final String header) {
        return Optional.ofNullable(header)
                .filter(StringUtils::hasText)
                .filter(authHeader -> authHeader.startsWith(BEARER_PREFIX))
                .map(authHeader -> authHeader.substring(BEARER_PREFIX.length()).trim())
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new AbsentBearerHeaderException(
                    STR."Missing or invalid Authorization header. Expected format: \{BEARER_PREFIX}<token>"
                ));
    }
}

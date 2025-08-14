package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.AbsentBearerHeaderException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

@Service
public class JwtTokenFromAuthHeaderExtractor {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    @Value("${jwt.header}")
    private String jwtHttpRequestHeader;

    public String extract(final HttpServletRequest request) {
        String header = request.getHeader(jwtHttpRequestHeader);
        return extract(header);
    }

    public String extract(final String header) {
        return Optional.ofNullable(header)
                .filter(authHeader -> authHeader.startsWith(BEARER_PREFIX))
                .map(authHeader -> StringUtils.substring(authHeader, BEARER_PREFIX_LENGTH))
                .orElseThrow(AbsentBearerHeaderException::new);
    }
}

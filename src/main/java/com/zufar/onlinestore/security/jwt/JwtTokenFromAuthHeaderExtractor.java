package com.zufar.onlinestore.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class JwtTokenFromAuthHeaderExtractor {
	public static final int JWT_TOKEN_START_POSITION_IN_HTTP_REQUEST_HEADER = 7;

	@Value("${jwt.header}")
	private String jwtHttpRequestHeader;

	public Optional<String> extract(final HttpServletRequest request) {
		final String authHeader = request.getHeader(jwtHttpRequestHeader);


		if (authHeader != null && authHeader.startsWith("Bearer "))
			return Optional.of(authHeader.substring(JWT_TOKEN_START_POSITION_IN_HTTP_REQUEST_HEADER));
		else
			return Optional.empty();
	}
}

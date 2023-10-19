package com.zufar.onlinestore.security.jwt;

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
		return extractAllClaims(jwtToken)
				.getSubject();
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

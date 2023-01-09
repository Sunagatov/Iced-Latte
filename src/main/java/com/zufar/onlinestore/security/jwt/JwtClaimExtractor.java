package com.zufar.onlinestore.security.jwt;

import org.springframework.stereotype.Service;

import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtClaimExtractor {
	private final JwtSignKeyProvider jwtSignKeyProvider;

	public String extractUsername(final String jwtToken) {
		return extractAllClaims(jwtToken)
				.getSubject();
	}

	public Date extractExpiration(final String jwtToken) {
		return extractAllClaims(jwtToken)
				.getExpiration();
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

package com.zufar.onlinestore.security.jwt;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Date;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtTokenValidator {
	private final JwtClaimExtractor jwtClaimExtractor;
	private final UserDetailsService userDetailsService;

	public boolean isValid(final String jwtToken) {
		final String userEmail = jwtClaimExtractor.extractUsername(jwtToken);

		final String usernameFromSystem = userDetailsService.loadUserByUsername(userEmail).getUsername();
		final String usernameFromJwtToken = jwtClaimExtractor.extractUsername(jwtToken);

		return !isTokenExpired(jwtToken)
				&& usernameFromJwtToken.equals(usernameFromSystem);
	}

	private boolean isTokenExpired(final String jwtToken) {
		Date now = new Date();
		return jwtClaimExtractor
				.extractExpiration(jwtToken)
				.before(now);
	}
}

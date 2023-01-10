package com.zufar.onlinestore.security.jwt;

import com.zufar.onlinestore.security.exception.JwtTokenException;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Date;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class JwtTokenValidator {
	private final JwtClaimExtractor jwtClaimExtractor;
	private final UserDetailsService userDetailsService;

	public boolean isValid(final String jwtToken) {
		try {
			final String userEmail = jwtClaimExtractor.extractUsername(jwtToken);

			final String usernameFromSystem = userDetailsService.loadUserByUsername(userEmail).getUsername();
			final String usernameFromJwtToken = jwtClaimExtractor.extractUsername(jwtToken);

			return !isTokenExpired(jwtToken)
					&& usernameFromJwtToken.equals(usernameFromSystem);
		} catch (Exception exception) {
			log.error("Jwt token validation error", exception);
			throw new JwtTokenException(exception);
		}
	}

	private boolean isTokenExpired(final String jwtToken) {
		Date now = new Date();
		return jwtClaimExtractor
				.extractExpiration(jwtToken)
				.before(now);
	}
}

package com.zufar.onlinestore.security.jwt.filter;

import com.zufar.onlinestore.security.exception.JwtTokenException;
import com.zufar.onlinestore.security.jwt.JwtClaimExtractor;
import com.zufar.onlinestore.security.jwt.JwtTokenFromAuthHeaderExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthenticationProvider {
	private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
	private final JwtClaimExtractor jwtClaimExtractor;
	private final UserDetailsService userDetailsService;

	public Optional<UsernamePasswordAuthenticationToken> get(final HttpServletRequest httpRequest) {
		Optional<String> jwtTokenOptional = jwtTokenFromAuthHeaderExtractor.extract(httpRequest);
		if (jwtTokenOptional.isEmpty()) {
			return Optional.empty();
		}

		final String jwtToken = jwtTokenOptional.get();

		try {
			Date expirationDate = jwtClaimExtractor.extractExpiration(jwtToken);
			Date now = new Date();
			if (expirationDate.before(now)) {
				throw new JwtTokenException("Jwt token is expired");
			}

			final String userEmail = jwtClaimExtractor.extractEmail(jwtToken);
			if (userEmail == null) {
				throw new JwtTokenException("User email not found in jwtToken");
			}

			UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

			UsernamePasswordAuthenticationToken authToken =
					new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));

			return Optional.of(authToken);

		} catch (Exception exception) {
			log.error("Jwt token validation error", exception);
			throw new JwtTokenException("Jwt token validation error", exception);
		}
	}
}

package com.zufar.onlinestore.security.jwt.filter;

import com.zufar.onlinestore.security.exception.JwtTokenException;
import com.zufar.onlinestore.security.jwt.JwtClaimExtractor;
import com.zufar.onlinestore.security.jwt.JwtTokenFromAuthHeaderExtractor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthenticationProvider {
	private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
	private final JwtClaimExtractor jwtClaimExtractor;
	private final UserDetailsService userDetailsService;

	public Optional<UsernamePasswordAuthenticationToken> get(final HttpServletRequest httpRequest) {
		final String jwtToken = jwtTokenFromAuthHeaderExtractor.extract(httpRequest).orElse(null);
		if (StringUtils.isEmpty(jwtToken)) {
			return Optional.empty();
		}

		try {
			LocalDateTime expirationDate = jwtClaimExtractor.extractExpiration(jwtToken);
			if (expirationDate.isBefore(LocalDateTime.now())) {
				throw new JwtTokenException("Jwt token is expired");
			}

			final String userEmail = jwtClaimExtractor.extractEmail(jwtToken);
			if (StringUtils.isEmpty(userEmail)) {
				throw new JwtTokenException("User email not found in jwtToken");
			}

			UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

			UsernamePasswordAuthenticationToken authToken =
					new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));

			return Optional.of(authToken);

		} catch (Exception exception) {
			log.error("Jwt token validation error", exception);
			return Optional.empty();
		}
	}
}

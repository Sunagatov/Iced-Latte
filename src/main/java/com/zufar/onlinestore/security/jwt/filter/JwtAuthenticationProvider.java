package com.zufar.onlinestore.security.jwt.filter;

import com.zufar.onlinestore.security.exception.JwtTokenException;
import com.zufar.onlinestore.security.jwt.JwtClaimExtractor;
import com.zufar.onlinestore.security.jwt.JwtTokenFromAuthHeaderExtractor;
import com.zufar.onlinestore.security.jwt.JwtTokenValidator;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthenticationProvider {
	private final JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
	private final JwtTokenValidator jwtTokenValidator;
	private final JwtClaimExtractor jwtClaimExtractor;
	private final UserDetailsService userDetailsService;

	public Optional<UsernamePasswordAuthenticationToken> get(final HttpServletRequest httpRequest) {
		try {

			//Try to retrieve token from Authentication Header of HttpServletRequest
			Optional<String> jwtTokenOptional = jwtTokenFromAuthHeaderExtractor.extract(httpRequest);
			if (jwtTokenOptional.isEmpty()) {
				return Optional.empty();
			}

			//Validate jwtToken
			final String jwtToken = jwtTokenOptional.get();
			if (!jwtTokenValidator.isValid(jwtToken)) {
				return Optional.empty();
			}

			//Try to retrieve userEmail from jwtToken
			final String userEmail = jwtClaimExtractor.extractUsername(jwtToken);
			if (userEmail == null || SecurityContextHolder.getContext().getAuthentication() != null) {
				return Optional.empty();
			}

			//Try to retrieve userDetails from system
			UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);


			//Create authToken
			UsernamePasswordAuthenticationToken authToken =
					new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpRequest));

			return Optional.of(authToken);
		} catch (Exception exception) {
			log.error("Jwt token validation error", exception);
			throw new JwtTokenException(exception);
		}
	}
}

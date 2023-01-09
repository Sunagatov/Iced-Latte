package com.zufar.onlinestore.security.jwt.filter;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private final JwtAuthenticationProvider jwtAuthenticationProvider;

	@Override
	protected void doFilterInternal(@NonNull final HttpServletRequest httpRequest,
	                                @NonNull final HttpServletResponse httpResponse,
	                                @NonNull final FilterChain filterChain) throws ServletException, IOException {
		Optional<UsernamePasswordAuthenticationToken> authenticationTokenOptional = jwtAuthenticationProvider.get(httpRequest);

		if (authenticationTokenOptional.isPresent()) {
			UsernamePasswordAuthenticationToken authenticationToken = authenticationTokenOptional.get();
			SecurityContextHolder
					.getContext()
					.setAuthentication(authenticationToken);
		}

		filterChain.doFilter(httpRequest, httpResponse);
	}
}

package com.zufar.onlinestore.security.jwt;

import com.zufar.onlinestore.security.exception.JwtTokenException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {
	private final JwtSignKeyProvider jwtSignKeyProvider;

	@Value("${jwt.expiration}")
	private long validityInMilliseconds;

	public String generateToken(final UserDetails userDetails) {
		return generateToken(new HashMap<>(), userDetails);
	}

	public String generateToken(final Map<String, Object> extraClaims,
	                            final UserDetails userDetails) {
		try {
			return Jwts.builder()
					.setClaims(extraClaims)
					.setSubject(userDetails.getUsername())
					.setIssuedAt(new Date(System.currentTimeMillis()))
					.setExpiration(new Date(System.currentTimeMillis() + validityInMilliseconds))
					.signWith(jwtSignKeyProvider.get(), SignatureAlgorithm.HS256)
					.compact();
		} catch (Exception exception) {
			log.error("Jwt token validation error", exception);
			throw new JwtTokenException(exception);
		}
	}
}

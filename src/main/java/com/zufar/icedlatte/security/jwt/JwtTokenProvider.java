package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenException;

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

	@Value("${jwt.refresh.expiration}")
	private long validityRefreshTokenInMilliseconds;

	public String generateToken(final UserDetails userDetails) {
		return generateToken(new HashMap<>(), userDetails);
	}

	private String generateToken(final Map<String, Object> extraClaims,
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
			log.debug("Jwt token creation error", exception);
			throw new JwtTokenException(exception);
		}
	}

	public String generateRefreshToken(final UserDetails userDetails) {
		try {
			return Jwts.builder()
					.setSubject(userDetails.getUsername())
					.setIssuedAt(new Date(System.currentTimeMillis()))
					.setExpiration(new Date(System.currentTimeMillis() + validityRefreshTokenInMilliseconds))
					.signWith(jwtSignKeyProvider.get(), SignatureAlgorithm.HS256)
					.compact();
		} catch (Exception exception) {
			log.debug("Jwt refresh token creation error", exception);
			throw new JwtTokenException(exception);
		}
	}
}

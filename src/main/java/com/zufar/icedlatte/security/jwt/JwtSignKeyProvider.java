package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Key;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
@RequiredArgsConstructor
public class JwtSignKeyProvider {

	private final JwtProperties jwtProperties;

	public Key get() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public Key getRefresh() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getRefreshSecret());
		return Keys.hmacShaKeyFor(keyBytes);
	}
}

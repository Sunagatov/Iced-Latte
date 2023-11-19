package com.zufar.icedlatte.security.exception;

import org.springframework.security.core.AuthenticationException;

public class JwtTokenBlacklistedException extends AuthenticationException {

	public JwtTokenBlacklistedException() {
		super("JWT Token is blacklisted");
	}
}

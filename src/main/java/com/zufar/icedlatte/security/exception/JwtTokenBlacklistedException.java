package com.zufar.icedlatte.security.exception;

import org.springframework.security.core.AuthenticationException;

public class JwtTokenBlacklistedException extends AuthenticationException {

	private static final String DEFAULT_MESSAGE = "JWT token has been revoked";

	public JwtTokenBlacklistedException() {
		super(DEFAULT_MESSAGE);
	}
	
	public JwtTokenBlacklistedException(String message) {
		super(message);
	}
	
	public JwtTokenBlacklistedException(String message, Throwable cause) {
		super(message, cause);
	}
}

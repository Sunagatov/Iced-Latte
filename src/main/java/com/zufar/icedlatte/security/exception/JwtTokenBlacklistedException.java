package com.zufar.icedlatte.security.exception;

import org.springframework.security.core.AuthenticationException;

public class JwtTokenBlacklistedException extends AuthenticationException {

    public JwtTokenBlacklistedException(String message) {
		super(message);
	}
}

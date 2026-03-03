package com.zufar.icedlatte.security.exception;

import org.springframework.security.core.AuthenticationException;

public class JwtTokenHasNoUserEmailException extends AuthenticationException {

	public JwtTokenHasNoUserEmailException(String message) {
		super(message);
	}

	public JwtTokenHasNoUserEmailException(String message, Throwable cause) {
		super(message, cause);
	}
}
